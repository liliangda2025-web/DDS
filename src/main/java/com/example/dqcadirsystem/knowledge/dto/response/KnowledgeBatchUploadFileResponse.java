package com.example.dqcadirsystem.knowledge.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 批量导入中单个文件的最终处理结果。
 *
 * <p>失败项不会返回已生成但未落库的内部 ID，避免前端把无效资源当作可访问记录；因此失败时
 * {@code entryId、fileId、fileUrl、infoStatus、uploadedAt} 均为 {@code null}。</p>
 */
public record KnowledgeBatchUploadFileResponse(
        int index,
        String entryId,
        String fileId,
        String fileName,
        String fileType,
        long fileSize,
        String fileUrl,
        String uploadStatus,
        Integer infoStatus,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime uploadedAt,
        String errorMsg) {

    /** 为已经完成数据库提交的文件创建成功结果。 */
    public static KnowledgeBatchUploadFileResponse success(
            int index,
            String entryId,
            String fileId,
            String fileName,
            String fileType,
            long fileSize,
            String fileUrl,
            LocalDateTime uploadedAt) {
        return new KnowledgeBatchUploadFileResponse(
                index, entryId, fileId, fileName, fileType, fileSize, fileUrl,
                "SUCCESS", 0, uploadedAt, null);
    }

    /** 为校验、OSS 或数据库处理失败的文件创建安全失败结果。 */
    public static KnowledgeBatchUploadFileResponse failed(
            int index,
            String fileName,
            String fileType,
            long fileSize,
            String errorMsg) {
        return new KnowledgeBatchUploadFileResponse(
                index, null, null, fileName, fileType, fileSize, null,
                "FAILED", null, null, errorMsg);
    }
}
