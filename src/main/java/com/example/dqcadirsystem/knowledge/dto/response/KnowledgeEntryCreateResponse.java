package com.example.dqcadirsystem.knowledge.dto.response;

/**
 * 新增知识条目成功后的响应数据。
 *
 * <p>数据库主键是 BIGINT，但接口以字符串返回，避免 JavaScript 对较大整数进行数值转换时丢失精度。</p>
 */
public record KnowledgeEntryCreateResponse(String entryId) {

    /** 将服务内部使用的 long 主键安全转换为接口字符串。 */
    public static KnowledgeEntryCreateResponse from(long entryId) {
        return new KnowledgeEntryCreateResponse(Long.toString(entryId));
    }
}
