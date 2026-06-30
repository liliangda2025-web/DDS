package com.example.dqcadirsystem.common.api;

import java.util.List;

/**
 * 所有分页接口共用的响应数据结构。
 *
 * <p>分页元数据和当前页记录统一放在 {@link ApiResponse} 的 {@code data} 字段中。使用不可变 record
 * 可以避免 Controller 返回结果后又被其他代码修改。</p>
 *
 * @param total    符合查询条件的总记录数
 * @param pageNum  当前页码，从 1 开始
 * @param pageSize 每页记录数
 * @param pages    总页数；没有记录时为 0
 * @param records  当前页记录；没有记录或页码超出范围时为空列表
 * @param <T>      当前接口的记录类型
 */
public record PageResponse<T>(long total, int pageNum, int pageSize, long pages, List<T> records) {

    /**
     * 根据总数和分页参数创建响应，并复制记录列表以保证外部不能修改。
     */
    public static <T> PageResponse<T> of(long total, int pageNum, int pageSize, List<T> records) {
        long pages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResponse<>(total, pageNum, pageSize, pages, List.copyOf(records));
    }
}
