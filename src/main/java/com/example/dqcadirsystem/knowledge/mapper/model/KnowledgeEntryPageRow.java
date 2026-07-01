package com.example.dqcadirsystem.knowledge.mapper.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mapper 查询知识条目分页列表时使用的数据库行投影。
 *
 * <p>该类型只表达数据库查询结果，不直接作为接口响应。类型名称、状态名称等展示字段由 Service 层
 * 根据统一枚举补充，从而避免把中文业务规则写进 SQL。</p>
 */
public record KnowledgeEntryPageRow(
        String entryId,
        String fileId,
        String entryType,
        String entryCode,
        String title,
        String secretLevel,
        String version,
        LocalDate releaseDate,
        String fileType,
        String professionCode,
        String authorName,
        Integer infoStatus,
        String originalFileName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
