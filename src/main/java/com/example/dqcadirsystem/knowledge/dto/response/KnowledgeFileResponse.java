package com.example.dqcadirsystem.knowledge.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 当前文件上传成功后的响应数据。
 *
 * <p>ID 使用字符串返回，避免前端 JavaScript Number 无法精确表示 19 位雪花 ID。</p>
 */
public record KnowledgeFileResponse(
        String fileId,
        String entryId,
        String originalFileName,
        String fileType,
        Long fileSize,
        String fileUrl,
        String uploadStatus,
        Integer isCurrent,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime uploadedAt) {
}
