package com.example.dqcadirsystem.knowledge.validation;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.knowledge.enums.KnowledgeFileType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

/**
 * 知识文件上传校验器。
 *
 * <p>扩展名和 Content-Type 都由客户端提供，不能作为真实格式的唯一依据。本类还会检查文件头或
 * 容器结构，阻止把脚本、图片等文件仅修改后缀后上传。校验阶段只读取少量头部数据；DOCX 的 ZIP
 * 遍历也设置条目数和解压字节上限，防止恶意压缩包消耗过多 CPU、内存或磁盘。</p>
 */
@Component
public class KnowledgeFileValidator {

    /** 业务允许的最大文件大小：500 MiB。 */
    public static final long MAX_FILE_SIZE = 500L * 1024 * 1024;

    private static final int GENERAL_HEADER_SIZE = 64 * 1024;
    private static final int MAX_DOCX_ENTRIES = 1000;
    private static final long MAX_DOCX_INSPECTED_BYTES = 16L * 1024 * 1024;
    private static final byte[] DOC_MAGIC = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };
    private static final byte[] BINARY_DXF_MAGIC =
            "AutoCAD Binary DXF\r\n\u001A\u0000".getBytes(StandardCharsets.US_ASCII);
    private static final Pattern DWG_VERSION = Pattern.compile("AC10\\d{2}");
    private static final Pattern ASCII_DXF_SECTION =
            Pattern.compile("(?m)^\\s*0\\s*\\n\\s*SECTION\\s*$", Pattern.CASE_INSENSITIVE);

    /**
     * 校验文件基础信息、客户端 MIME 和真实内容特征，并返回标准化元数据。
     */
    public ValidatedKnowledgeFile validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw badRequest("文件大小不能超过500MB");
        }

        String originalFileName = normalizeOriginalFileName(file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(originalFileName);
        KnowledgeFileType type = KnowledgeFileType.fromExtension(extension);
        if (type == null) {
            throw badRequest("仅支持PDF、Word、DWG和DXF文件");
        }
        if (!type.supportsClientContentType(file.getContentType())) {
            throw badRequest("文件类型与Content-Type不一致");
        }

        try {
            boolean signatureMatches = switch (type) {
                case PDF -> isPdf(file);
                case DOC -> isLegacyWord(file);
                case DOCX -> isDocx(file);
                case DWG -> isDwg(file);
                case DXF -> isDxf(file);
            };
            if (!signatureMatches) {
                throw badRequest("文件内容与扩展名不一致或文件已损坏");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw badRequest("无法读取上传文件");
        }
        return new ValidatedKnowledgeFile(originalFileName, type, file.getSize());
    }

    /** 去除浏览器可能携带的本地路径，并保证名称能写入 VARCHAR(255)。 */
    private String normalizeOriginalFileName(String suppliedName) {
        if (suppliedName == null || suppliedName.isBlank()) {
            throw badRequest("文件名不能为空");
        }
        String cleaned = StringUtils.cleanPath(suppliedName.trim());
        String filename = StringUtils.getFilename(cleaned);
        if (filename == null || filename.isBlank() || filename.length() > 255) {
            throw badRequest("文件名不能为空且不能超过255个字符");
        }
        return filename;
    }

    /** PDF 标识通常位于开头，规范允许它出现在前 1024 个字节内。 */
    private boolean isPdf(MultipartFile file) throws IOException {
        byte[] header = readPrefix(file, 1024);
        return indexOf(header, "%PDF-".getBytes(StandardCharsets.US_ASCII)) >= 0;
    }

    /** 旧版 DOC 使用 OLE Compound File 固定的八字节魔数。 */
    private boolean isLegacyWord(MultipartFile file) throws IOException {
        return startsWith(readPrefix(file, DOC_MAGIC.length), DOC_MAGIC);
    }

    /**
     * DOCX 本质是 Open Packaging Convention ZIP，必须同时具备内容类型清单和 Word 主目录。
     */
    private boolean isDocx(MultipartFile file) throws IOException {
        boolean hasContentTypes = false;
        boolean hasWordDirectory = false;
        int entryCount = 0;
        long inspectedBytes = 0;
        byte[] buffer = new byte[8192];

        try (InputStream inputStream = file.getInputStream();
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_DOCX_ENTRIES) {
                    throw badRequest("Word文件包含过多压缩条目");
                }
                String entryName = entry.getName().replace('\\', '/');
                hasContentTypes |= "[Content_Types].xml".equals(entryName);
                hasWordDirectory |= entryName.startsWith("word/");
                if (hasContentTypes && hasWordDirectory) {
                    return true;
                }

                // 有界读取当前小型结构条目，避免 getNextEntry 隐式展开一个超大压缩条目。
                int read;
                while ((read = zipInputStream.read(buffer)) != -1) {
                    inspectedBytes += read;
                    if (inspectedBytes > MAX_DOCX_INSPECTED_BYTES) {
                        throw badRequest("Word文件结构异常或压缩内容过大");
                    }
                }
            }
            return false;
        } catch (ZipException exception) {
            return false;
        }
    }

    /** DWG 文件以六字节 AC10xx 版本号开头，例如 AC1027。 */
    private boolean isDwg(MultipartFile file) throws IOException {
        byte[] header = readPrefix(file, 6);
        return header.length == 6
                && DWG_VERSION.matcher(new String(header, StandardCharsets.US_ASCII)).matches();
    }

    /** 同时识别二进制 DXF 固定标识和 ASCII DXF 的 SECTION 起始组。 */
    private boolean isDxf(MultipartFile file) throws IOException {
        byte[] header = readPrefix(file, GENERAL_HEADER_SIZE);
        if (startsWith(header, BINARY_DXF_MAGIC)) {
            return true;
        }
        String text = new String(header, StandardCharsets.ISO_8859_1)
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        return ASCII_DXF_SECTION.matcher(text).find();
    }

    /** 只读取验证所需的文件前缀，绝不把完整大文件加载进内存。 */
    private byte[] readPrefix(MultipartFile file, int limit) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(limit);
        }
    }

    private boolean startsWith(byte[] source, byte[] prefix) {
        return source.length >= prefix.length
                && Arrays.equals(Arrays.copyOf(source, prefix.length), prefix);
    }

    private int indexOf(byte[] source, byte[] target) {
        for (int i = 0; i <= source.length - target.length; i++) {
            boolean matches = true;
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return i;
            }
        }
        return -1;
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(CommonErrorCode.BAD_REQUEST, message);
    }
}
