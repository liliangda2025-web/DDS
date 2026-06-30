package com.example.dqcadirsystem.knowledge.mapper.model;

import java.time.LocalDateTime;

/**
 * knowledge_file 表的上传结果投影，只包含接口响应需要的字段。
 */
public record KnowledgeFileRow(
        String fileId,
        String entryId,
        String originalFileName,
        String fileType,
        Long fileSize,
        String fileUrl,
        String uploadStatus,
        Integer isCurrent,
        LocalDateTime uploadedAt) {
}
