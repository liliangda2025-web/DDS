package com.example.dqcadirsystem.knowledge.dto.response;

import java.util.List;

/** 批量导入知识条目信息的汇总结果。 */
public record KnowledgeEntryInfoImportResponse(
        int totalCount,
        int successCount,
        int failedCount,
        List<KnowledgeEntryInfoImportFailureResponse> failedList) {

    public KnowledgeEntryInfoImportResponse {
        failedList = List.copyOf(failedList);
    }

    /** 根据总数、成功数和失败列表生成计数一致的响应。 */
    public static KnowledgeEntryInfoImportResponse of(
            int totalCount,
            int successCount,
            List<KnowledgeEntryInfoImportFailureResponse> failedList) {
        return new KnowledgeEntryInfoImportResponse(
                totalCount, successCount, failedList.size(), failedList);
    }
}
