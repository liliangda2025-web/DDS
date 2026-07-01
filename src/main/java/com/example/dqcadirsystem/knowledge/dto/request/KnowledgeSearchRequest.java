package com.example.dqcadirsystem.knowledge.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 知识检索请求参数。
 *
 * <p>当前版本使用数据库字段模糊匹配，不承载语义检索任务 ID 或匹配度参数。构造阶段统一清理文本并补充分页
 * 默认值，保证 Controller、Service 和 Mapper 使用同一份稳定数据。</p>
 */
public record KnowledgeSearchRequest(
        @NotBlank(message = "查询内容不能为空")
        @Size(max = 200, message = "查询内容长度不能超过200个字符")
        String queryText,

        @Size(max = 50, message = "知识条目类型长度不能超过50个字符")
        String entryType,

        @Min(value = 1, message = "页码必须大于等于1")
        Integer pageNum,

        @Min(value = 1, message = "每页条数必须大于等于1")
        @Max(value = 100, message = "每页条数不能超过100")
        Integer pageSize) {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    public KnowledgeSearchRequest {
        queryText = trimToNull(queryText);
        entryType = trimToNull(entryType);
        pageNum = pageNum == null ? DEFAULT_PAGE_NUM : pageNum;
        pageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
    }

    /** 计算 MySQL LIMIT 的起始位置，使用 long 避免大页码乘法溢出。 */
    public long offset() {
        return (long) (pageNum - 1) * pageSize;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
