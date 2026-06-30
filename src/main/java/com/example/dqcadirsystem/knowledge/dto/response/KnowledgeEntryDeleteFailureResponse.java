package com.example.dqcadirsystem.knowledge.dto.response;

/**
 * 批量删除中单个未删除条目的失败明细。
 *
 * <p>ID 继续以字符串返回，失败原因是可安全展示给调用方的业务提示。</p>
 */
public record KnowledgeEntryDeleteFailureResponse(String entryId, String reason) {

    /** 创建“条目不存在或已删除”的标准失败明细。 */
    public static KnowledgeEntryDeleteFailureResponse notFound(Long entryId) {
        return new KnowledgeEntryDeleteFailureResponse(Long.toString(entryId), "知识条目不存在");
    }
}
