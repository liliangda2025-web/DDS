package com.example.dqcadirsystem.knowledge.dto.response;

import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeEntryDetailRow;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 知识条目详情中的当前文件信息。
 *
 * <p>文件 ID 与条目 ID 一样以字符串返回，保证前端可以无损处理数据库 BIGINT。文件大小的单位固定为字节，
 * 前端可按展示需要换算为 KB、MB 等单位。</p>
 */
public record KnowledgeCurrentFileResponse(
        String fileId,
        String originalFileName,
        String fileType,
        Long fileSize,
        String fileUrl,
        String uploadStatus,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime uploadedAt) {

    /**
     * 从详情查询结果中提取当前文件字段。
     *
     * <p>调用方只会在 {@code fileId} 非空时调用本方法，因此这里返回的对象一定代表一条真实存在的文件记录。</p>
     */
    public static KnowledgeCurrentFileResponse from(KnowledgeEntryDetailRow row) {
        return new KnowledgeCurrentFileResponse(
                row.fileId(),
                row.originalFileName(),
                row.fileType(),
                row.fileSize(),
                row.fileUrl(),
                row.uploadStatus(),
                row.uploadedAt());
    }
}
