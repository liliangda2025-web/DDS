package com.example.dqcadirsystem.knowledge.dto.response;

import java.util.List;

/**
 * 批量导入文件汇总响应。
 *
 * <p>{@code fileList} 使用不可变副本，防止响应创建后被业务代码误改，确保三个计数字段与明细一致。</p>
 */
public record KnowledgeBatchUploadResponse(
        int totalCount,
        int successCount,
        int failedCount,
        List<KnowledgeBatchUploadFileResponse> fileList) {

    public KnowledgeBatchUploadResponse {
        fileList = List.copyOf(fileList);
    }
}
