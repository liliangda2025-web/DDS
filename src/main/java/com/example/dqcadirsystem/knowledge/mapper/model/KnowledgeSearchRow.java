package com.example.dqcadirsystem.knowledge.mapper.model;

/** 数据库知识检索结果投影，不直接作为外部响应。 */
public record KnowledgeSearchRow(
        String entryId,
        String fileId,
        String entryType,
        String entryCode,
        String title,
        String keywords,
        String projectName,
        String systemSource,
        String professionCode,
        String authorName,
        String originalFileName,
        String fileType) {
}
