package com.example.dqcadirsystem.knowledge.mapper.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 知识条目信息补充模板的一行数据库投影。
 *
 * <p>该投影只包含 Excel 需要的字段，刻意不查询 {@code file_url}，从数据访问层避免文件地址
 * 被意外写入模板。两个 19 位 ID 在 SQL 中转换为字符串，防止进入 Excel 后丢失精度。</p>
 */
public record KnowledgeSupplementTemplateRow(
        String entryId,
        String fileId,
        String originalFileName,
        String fileType,
        Long fileSize,
        LocalDateTime uploadedAt,
        String uploadStatus,
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
        String secretLevel) {
}
