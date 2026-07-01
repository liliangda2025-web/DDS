package com.example.dqcadirsystem.knowledge.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 导出知识条目信息补充模板的请求条件。
 *
 * <p>请求支持“精确 ID”和“条件筛选”两种模式，二者互斥。DTO 只负责基础结构校验和文本规范化，
 * 模式互斥、ID 去重、知识类型及时间先后关系由 Service 统一校验，以便返回稳定的业务提示。</p>
 */
public record KnowledgeSupplementTemplateExportRequest(
        @Size(max = 1000, message = "精确导出最多选择1000条知识条目")
        List<@NotNull(message = "知识条目ID不能为空")
                @Positive(message = "知识条目ID必须大于0") Long> entryIds,
        String entryType,
        String fileName,
        LocalDateTime uploadStartTime,
        LocalDateTime uploadEndTime) {

    /**
     * 规范化可选文本，并复制 ID 列表，防止 Controller 绑定完成后外部继续修改请求数据。
     */
    public KnowledgeSupplementTemplateExportRequest {
        if (entryIds != null) {
            // 不能使用 List.copyOf：其中的 null 必须交给 Bean Validation 返回约定的中文提示。
            entryIds = Collections.unmodifiableList(new ArrayList<>(entryIds));
        }
        entryType = trimToNull(entryType);
        fileName = trimToNull(fileName);
    }

    /** ID 列表非空时进入精确导出模式；空数组与未传含义一致。 */
    public boolean exactMode() {
        return entryIds != null && !entryIds.isEmpty();
    }

    /** 判断请求中是否声明了任意筛选条件，用于阻止两种查询模式混用。 */
    public boolean hasFilterCondition() {
        return entryType != null || fileName != null
                || uploadStartTime != null || uploadEndTime != null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
