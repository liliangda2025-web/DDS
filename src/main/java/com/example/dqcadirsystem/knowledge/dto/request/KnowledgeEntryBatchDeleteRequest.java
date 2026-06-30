package com.example.dqcadirsystem.knowledge.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 批量删除知识条目的请求参数。
 *
 * <p>列表本身不能为空，每个 ID 也必须是非空正整数。接口文档使用 string 表示 ID，是为了避免前端数值精度丢失；
 * Jackson 可以把 JSON 中的数字字符串安全转换为后端 {@link Long}。</p>
 */
public record KnowledgeEntryBatchDeleteRequest(
        @NotEmpty(message = "知识条目ID列表不能为空")
        List<@NotNull(message = "知识条目ID不能为空")
                @Positive(message = "知识条目ID必须大于0") Long> entryIds) {

    /**
     * 使用不可变副本保存调用方数据，避免进入 Service 后列表被外部代码修改。
     */
    public KnowledgeEntryBatchDeleteRequest {
        if (entryIds != null) {
            // 不使用 List.copyOf，因为它会在 Bean Validation 运行前拒绝 null 元素，导致校验提示不准确。
            entryIds = Collections.unmodifiableList(new ArrayList<>(entryIds));
        }
    }
}
