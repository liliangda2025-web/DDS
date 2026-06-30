package com.example.dqcadirsystem.knowledge.dto.response;

import com.example.dqcadirsystem.knowledge.enums.KnowledgeEntryInfoStatus;
import com.example.dqcadirsystem.knowledge.enums.KnowledgeEntryType;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeEntryPageRow;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 知识条目管理列表中的一行响应数据。
 *
 * <p>ID 使用 String，避免 JavaScript 无法精确表示 19 位 BIGINT。时间格式通过 Jackson 注解固定为
 * 接口文档中的 {@code yyyy-MM-dd HH:mm:ss}。</p>
 */
public record KnowledgeEntryPageItemResponse(
        String entryId,
        String fileId,
        String entryType,
        String entryTypeName,
        String entryCode,
        String title,
        String secretLevel,
        String version,
        LocalDate releaseDate,
        String fileType,
        String professionCode,
        String authorName,
        Integer infoStatus,
        String infoStatusName,
        String originalFileName,
        String fileUrl,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime updatedAt) {

    /**
     * 将数据库行投影转换为接口响应，并统一补充枚举中文名称。
     */
    public static KnowledgeEntryPageItemResponse from(KnowledgeEntryPageRow row) {
        KnowledgeEntryType entryType = KnowledgeEntryType.fromValue(row.entryType())
                .orElseThrow(() -> new IllegalArgumentException("未知知识条目类型: " + row.entryType()));
        return new KnowledgeEntryPageItemResponse(
                row.entryId(),
                row.fileId(),
                entryType.value(),
                entryType.label(),
                row.entryCode(),
                row.title(),
                row.secretLevel(),
                row.version(),
                row.releaseDate(),
                row.fileType(),
                row.professionCode(),
                row.authorName(),
                row.infoStatus(),
                KnowledgeEntryInfoStatus.labelOf(row.infoStatus()),
                row.originalFileName(),
                row.fileUrl(),
                row.createdAt(),
                row.updatedAt());
    }
}
