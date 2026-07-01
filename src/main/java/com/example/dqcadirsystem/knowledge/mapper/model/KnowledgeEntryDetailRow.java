package com.example.dqcadirsystem.knowledge.mapper.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 知识条目详情查询的数据库行投影。
 *
 * <p>该类型只在 Mapper 与 Service 之间传递数据，不直接暴露给前端。条目字段和当前文件字段被平铺在同一行中，
 * 可以通过一次左连接查询完成详情读取；当条目尚无当前有效文件时，所有文件字段均为 {@code null}。</p>
 *
 * <p>{@code entryId} 和 {@code fileId} 使用字符串承接数据库 BIGINT，避免 19 位 ID 在 JavaScript 中出现精度丢失。</p>
 */
public record KnowledgeEntryDetailRow(
        String entryId,
        String entryType,
        String entryCode,
        String title,
        String keywords,
        String version,
        String projectName,
        LocalDate releaseDate,
        String systemSource,
        String professionCode,
        String authorName,
        String secretLevel,
        Integer infoStatus,
        String fileId,
        String originalFileName,
        String fileType,
        Long fileSize,
        String uploadStatus,
        LocalDateTime uploadedAt) {
}
