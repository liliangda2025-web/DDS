package com.example.dqcadirsystem.knowledge.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

/**
 * 分页查询知识条目的请求参数。
 *
 * <p>构造时会把纯空白文本转换为 {@code null}，避免空字符串生成无意义的 SQL 条件；分页参数未传时
 * 分别使用接口文档约定的第 1 页和每页 10 条。</p>
 */
public record KnowledgeEntryPageRequest(
        String entryType,
        String fileName,
        String entryCode,
        String title,
        String professionCode,
        @Min(value = 0, message = "信息完善状态只能为0或1")
        @Max(value = 1, message = "信息完善状态只能为0或1")
        Integer infoStatus,
        LocalDate startDate,
        LocalDate endDate,
        @Min(value = 1, message = "页码必须大于等于1")
        Integer pageNum,
        @Min(value = 1, message = "每页条数必须大于等于1")
        Integer pageSize) {

    /** 默认页码。 */
    private static final int DEFAULT_PAGE_NUM = 1;

    /** 默认每页条数。 */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 规范化请求数据，保证后续 Service 和 Mapper 接收到稳定值。
     */
    public KnowledgeEntryPageRequest {
        entryType = trimToNull(entryType);
        fileName = trimToNull(fileName);
        entryCode = trimToNull(entryCode);
        title = trimToNull(title);
        professionCode = trimToNull(professionCode);
        pageNum = pageNum == null ? DEFAULT_PAGE_NUM : pageNum;
        pageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
    }

    /**
     * 计算 MySQL {@code LIMIT} 使用的起始偏移量。
     * 使用 long 计算可以避免较大页码相乘时发生 int 溢出。
     */
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
