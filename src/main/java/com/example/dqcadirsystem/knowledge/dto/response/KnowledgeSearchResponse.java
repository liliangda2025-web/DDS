package com.example.dqcadirsystem.knowledge.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** 知识检索分页、耗时和卡片结果。 */
public record KnowledgeSearchResponse(
        long total,
        int pageNum,
        int pageSize,
        long pages,
        BigDecimal costSeconds,
        String messageText,
        List<KnowledgeSearchItemResponse> records) {

    public KnowledgeSearchResponse {
        records = List.copyOf(records);
    }
}
