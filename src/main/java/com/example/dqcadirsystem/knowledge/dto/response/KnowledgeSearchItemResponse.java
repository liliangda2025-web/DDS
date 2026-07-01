package com.example.dqcadirsystem.knowledge.dto.response;

import com.example.dqcadirsystem.knowledge.enums.KnowledgeEntryType;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeSearchRow;

/** 知识检索页面的一张结果卡片。 */
public record KnowledgeSearchItemResponse(
        String entryId,
        String fileId,
        String entryType,
        String entryTypeName,
        String entryCode,
        String displayName,
        String title,
        String keywords,
        String projectName,
        String systemSource,
        String professionCode,
        String authorName,
        String originalFileName,
        String fileType) {

    /** 补充类型中文名，并按照“标题优先、文件名兜底”生成卡片名称。 */
    public static KnowledgeSearchItemResponse from(KnowledgeSearchRow row) {
        KnowledgeEntryType type = KnowledgeEntryType.fromValue(row.entryType())
                .orElseThrow(() -> new IllegalArgumentException("未知知识条目类型: " + row.entryType()));
        String displayName = row.title() == null || row.title().isBlank()
                ? row.originalFileName() : row.title();
        return new KnowledgeSearchItemResponse(
                row.entryId(), row.fileId(), type.value(), type.label(), row.entryCode(), displayName,
                row.title(), row.keywords(), row.projectName(), row.systemSource(), row.professionCode(),
                row.authorName(), row.originalFileName(), row.fileType());
    }
}
