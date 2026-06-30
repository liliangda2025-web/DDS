package com.example.dqcadirsystem.knowledge.enums;

import java.util.Locale;
import java.util.Set;

/**
 * 当前知识库允许上传的文件类型。
 *
 * <p>扩展名用于接口展示和 OSS Key，标准 Content-Type 用于 OSS 响应头；浏览器上报的 MIME
 * 仅作辅助校验，因为不同操作系统对 DWG、DXF 和旧版 Word 的识别结果并不完全一致。</p>
 */
public enum KnowledgeFileType {

    PDF("pdf", "application/pdf", Set.of("application/pdf")),
    DOC("doc", "application/msword", Set.of("application/msword", "application/x-ole-storage")),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/zip", "application/x-zip-compressed")),
    DWG("dwg", "application/dwg",
            Set.of("application/dwg", "application/x-dwg", "application/acad", "application/x-acad",
                    "image/vnd.dwg")),
    DXF("dxf", "application/dxf",
            Set.of("application/dxf", "application/x-dxf", "image/vnd.dxf", "text/plain"));

    /** 浏览器无法判断文件类型时常用的通用 MIME，对全部支持格式都允许。 */
    private static final String GENERIC_BINARY_MIME = "application/octet-stream";

    private final String extension;
    private final String contentType;
    private final Set<String> compatibleClientContentTypes;

    KnowledgeFileType(String extension, String contentType, Set<String> compatibleClientContentTypes) {
        this.extension = extension;
        this.contentType = contentType;
        this.compatibleClientContentTypes = compatibleClientContentTypes;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    /**
     * 根据小写或大写扩展名查找文件类型。
     *
     * @return 匹配类型；扩展名不受支持时返回 {@code null}
     */
    public static KnowledgeFileType fromExtension(String extension) {
        if (extension == null) {
            return null;
        }
        String normalized = extension.toLowerCase(Locale.ROOT);
        for (KnowledgeFileType type : values()) {
            if (type.extension.equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断客户端声明的 MIME 是否与扩展名基本兼容。
     *
     * <p>空 MIME 和通用二进制 MIME 被接受，真实类型最终仍由文件特征校验决定。</p>
     */
    public boolean supportsClientContentType(String clientContentType) {
        if (clientContentType == null || clientContentType.isBlank()) {
            return true;
        }
        String normalized = clientContentType.toLowerCase(Locale.ROOT);
        int parameterStart = normalized.indexOf(';');
        if (parameterStart >= 0) {
            normalized = normalized.substring(0, parameterStart).trim();
        }
        return GENERIC_BINARY_MIME.equals(normalized) || compatibleClientContentTypes.contains(normalized);
    }
}
