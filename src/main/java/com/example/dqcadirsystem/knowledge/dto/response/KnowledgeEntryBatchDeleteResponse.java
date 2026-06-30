package com.example.dqcadirsystem.knowledge.dto.response;

import java.util.List;

/**
 * 批量删除知识条目的处理结果。
 *
 * <p>业务层面的不存在不会让整批操作失败：正常条目计入成功数，不存在或已删除条目进入失败列表。
 * 数据库异常不属于业务失败，会触发事务回滚并由全局异常处理器返回系统错误。</p>
 */
public record KnowledgeEntryBatchDeleteResponse(
        int successCount,
        int failedCount,
        List<KnowledgeEntryDeleteFailureResponse> failedList) {

    /**
     * 根据成功数量和失败明细创建稳定的响应对象。
     */
    public static KnowledgeEntryBatchDeleteResponse of(
            int successCount, List<KnowledgeEntryDeleteFailureResponse> failedList) {
        List<KnowledgeEntryDeleteFailureResponse> immutableFailedList = List.copyOf(failedList);
        return new KnowledgeEntryBatchDeleteResponse(
                successCount,
                immutableFailedList.size(),
                immutableFailedList);
    }
}
