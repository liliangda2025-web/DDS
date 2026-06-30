package com.example.dqcadirsystem.knowledge.dto.response;

import com.example.dqcadirsystem.knowledge.enums.KnowledgeEntryInfoStatus;
import com.example.dqcadirsystem.knowledge.enums.KnowledgeEntryType;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeEntryDetailRow;

import java.time.LocalDate;

/**
 * 查看知识条目详情接口的响应数据。
 *
 * <p>响应只包含接口文档约定的业务字段，不直接序列化数据库实体或内部枚举，避免后续表结构调整意外改变接口契约。</p>
 */
public record KnowledgeEntryDetailResponse(
        String entryId,
        String entryType,
        String entryTypeName,
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
        String infoStatusName,
        KnowledgeCurrentFileResponse currentFile) {

    /**
     * 将数据库查询结果转换为稳定的接口响应。
     *
     * <p>类型名称和完善状态名称统一从枚举获得，防止多个接口各自维护中文文案。条目没有当前文件时，
     * {@code currentFile} 明确返回 {@code null}，而不是返回字段全为空的对象。</p>
     */
    public static KnowledgeEntryDetailResponse from(KnowledgeEntryDetailRow row) {
        KnowledgeEntryType entryType = KnowledgeEntryType.fromValue(row.entryType())
                .orElseThrow(() -> new IllegalArgumentException("未知知识条目类型: " + row.entryType()));
        KnowledgeCurrentFileResponse currentFile = row.fileId() == null
                ? null
                : KnowledgeCurrentFileResponse.from(row);

        return new KnowledgeEntryDetailResponse(
                row.entryId(),
                entryType.value(),
                entryType.label(),
                row.entryCode(),
                row.title(),
                row.keywords(),
                row.version(),
                row.projectName(),
                row.releaseDate(),
                row.systemSource(),
                row.professionCode(),
                row.authorName(),
                row.secretLevel(),
                row.infoStatus(),
                KnowledgeEntryInfoStatus.labelOf(row.infoStatus()),
                currentFile);
    }
}
