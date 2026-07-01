package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.common.id.LongIdGenerator;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeBatchUploadFileResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeBatchUploadResponse;
import com.example.dqcadirsystem.knowledge.enums.KnowledgeEntryType;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeFileRow;
import com.example.dqcadirsystem.knowledge.storage.StoredObject;
import com.example.dqcadirsystem.knowledge.validation.KnowledgeFileValidator;
import com.example.dqcadirsystem.knowledge.validation.ValidatedKnowledgeFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 批量导入文件的非事务编排服务。
 *
 * <p>请求级规则在处理任何文件前一次性校验；通过后严格按 multipart 顺序同步处理。外层不建立
 * 长事务，避免 OSS 网络耗时占用数据库连接，同时保证某个文件失败时仍可继续处理后续文件。</p>
 */
@Service
public class KnowledgeBatchUploadService {

    /** 单次批量最多接收的文件数量。 */
    public static final int MAX_FILE_COUNT = 10;

    /** 单次批量所有文件声明大小之和最大为 1 GiB。 */
    public static final long MAX_TOTAL_SIZE = 1024L * 1024 * 1024;

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBatchUploadService.class);
    private static final String SYSTEM_FAILURE_MESSAGE = "文件处理失败，请稍后重试";

    private final KnowledgeFileValidator fileValidator;
    private final LongIdGenerator longIdGenerator;
    private final KnowledgeFileStorageCoordinator storageCoordinator;
    private final KnowledgeBatchUploadPersistenceService persistenceService;

    public KnowledgeBatchUploadService(
            KnowledgeFileValidator fileValidator,
            LongIdGenerator longIdGenerator,
            KnowledgeFileStorageCoordinator storageCoordinator,
            KnowledgeBatchUploadPersistenceService persistenceService) {
        this.fileValidator = fileValidator;
        this.longIdGenerator = longIdGenerator;
        this.storageCoordinator = storageCoordinator;
        this.persistenceService = persistenceService;
    }

    /**
     * 校验批次并逐个处理文件，最终返回与请求顺序一致的完整结果列表。
     */
    public KnowledgeBatchUploadResponse upload(String entryTypeValue, List<MultipartFile> files) {
        KnowledgeEntryType entryType = parseEntryType(entryTypeValue);
        validateBatch(files);

        List<KnowledgeBatchUploadFileResponse> results = new ArrayList<>(files.size());
        int successCount = 0;
        for (int i = 0; i < files.size(); i++) {
            KnowledgeBatchUploadFileResponse result = processOne(i + 1, entryType, files.get(i));
            results.add(result);
            if ("SUCCESS".equals(result.uploadStatus())) {
                successCount++;
            }
        }
        return new KnowledgeBatchUploadResponse(
                files.size(), successCount, files.size() - successCount, results);
    }

    /**
     * 处理单个文件。所有可预期业务错误只影响当前项，未知系统错误记录完整堆栈后返回安全提示。
     */
    private KnowledgeBatchUploadFileResponse processOne(
            int index,
            KnowledgeEntryType entryType,
            MultipartFile multipartFile) {
        String displayName = displayFileName(multipartFile);
        String displayType = displayFileType(displayName);
        long displaySize = multipartFile == null ? 0L : Math.max(0L, multipartFile.getSize());
        StoredObject storedObject = null;

        try {
            ValidatedKnowledgeFile file = fileValidator.validate(multipartFile);
            String title = extractTitle(file.originalFileName());
            long entryId = longIdGenerator.nextId();
            long fileId = longIdGenerator.nextId();

            storedObject = storageCoordinator.upload(entryId, fileId, multipartFile, file);
            KnowledgeFileRow row = persistenceService.createPlaceholderEntryAndFile(
                    entryId, fileId, entryType, title, file, storedObject);
            return KnowledgeBatchUploadFileResponse.success(
                    index,
                    row.entryId(),
                    row.fileId(),
                    row.originalFileName(),
                    row.fileType(),
                    row.fileSize(),
                    row.fileUrl(),
                    row.uploadedAt());
        } catch (BusinessException exception) {
            // 文件校验和 OSS 失败都已经转换为可安全展示的业务提示。
            // 正常情况下这两类错误发生时 storedObject 仍为空；保留判断可防御未来持久化层
            // 新增业务校验后，OSS 已成功却未执行补偿的回归问题。
            if (storedObject != null) {
                storageCoordinator.compensate(storedObject, exception);
            }
            return KnowledgeBatchUploadFileResponse.failed(
                    index, displayName, displayType, displaySize, exception.getMessage());
        } catch (RuntimeException exception) {
            if (storedObject != null) {
                storageCoordinator.compensate(storedObject, exception);
            }
            log.error("Batch knowledge file processing failed: index={}, fileName={}",
                    index, displayName, exception);
            return KnowledgeBatchUploadFileResponse.failed(
                    index, displayName, displayType, displaySize, SYSTEM_FAILURE_MESSAGE);
        }
    }

    /** entryType 去除首尾空白后仍严格按固定大写业务编码匹配。 */
    private KnowledgeEntryType parseEntryType(String value) {
        if (value == null || value.isBlank()) {
            throw badRequest("知识条目类型不能为空");
        }
        return KnowledgeEntryType.fromValue(value.trim())
                .orElseThrow(() -> badRequest("知识条目类型不正确"));
    }

    /**
     * 请求级校验必须在上传第一个 OSS 对象前完成，防止发现数量或总大小不合法时留下部分结果。
     */
    private void validateBatch(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw badRequest("批量上传文件不能为空");
        }
        if (files.size() > MAX_FILE_COUNT) {
            throw badRequest("单次批量上传不能超过10个文件");
        }

        long totalSize = 0L;
        for (MultipartFile file : files) {
            long size = file == null ? 0L : Math.max(0L, file.getSize());
            // 使用减法比较避免极端伪造 MultipartFile 大小相加时发生 long 溢出。
            if (size > MAX_TOTAL_SIZE - totalSize) {
                throw badRequest("单次批量上传总大小不能超过1GiB");
            }
            totalSize += size;
        }
    }

    /** 去掉最后一个扩展名，作为批量导入占位条目的标题。 */
    private String extractTitle(String originalFileName) {
        String extension = StringUtils.getFilenameExtension(originalFileName);
        String title = extension == null
                ? originalFileName
                : originalFileName.substring(0, originalFileName.length() - extension.length() - 1);
        if (title.isBlank()) {
            throw badRequest("文件名去除扩展名后不能为空");
        }
        return title;
    }

    /** 失败结果只展示基础文件名，不回显客户端可能携带的本地目录。 */
    private String displayFileName(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null) {
            return null;
        }
        String filename = StringUtils.getFilename(StringUtils.cleanPath(file.getOriginalFilename().trim()));
        if (filename == null) {
            return null;
        }
        return filename.length() <= 255 ? filename : filename.substring(0, 255);
    }

    /** 从展示文件名中提取小写扩展名；未知或无扩展名时返回 {@code null}。 */
    private String displayFileType(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        return extension == null || extension.isBlank() ? null : extension.toLowerCase(Locale.ROOT);
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(CommonErrorCode.BAD_REQUEST, message);
    }
}
