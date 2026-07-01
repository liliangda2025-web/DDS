package com.example.dqcadirsystem.knowledge.service.model;

import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryBusinessKey;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryUpdateRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 从知识条目信息补充模板解析得到的一行数据。
 *
 * <p>同时保留 ID 原始文本和转换后的 Long：原始文本用于失败结果展示，Long 用于数据库定位。
 * {@code parseError} 只保存解析阶段遇到的第一条稳定提示，后续业务编排仍可以统计并返回该行失败，
 * 而不会因为一行格式问题中止整个工作簿。</p>
 */
public record KnowledgeEntryInfoImportRow(
        int rowNum,
        String entryIdText,
        Long entryId,
        String fileIdText,
        Long fileId,
        String fileName,
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
        String secretLevel,
        String parseError) implements KnowledgeEntryBusinessKey {

    /** 将已校验的可填写字段转换为现有修改 SQL 使用的请求模型。 */
    public KnowledgeEntryUpdateRequest toUpdateRequest() {
        return new KnowledgeEntryUpdateRequest(
                entryType, entryCode, title, keywords, version, projectName, releaseDate,
                systemSource, professionCode, authorName, secretLevel);
    }
}
