package com.example.dqcadirsystem.knowledge.mapper.model;

/**
 * 文件预览查询的数据库投影。
 *
 * <p>{@code fileUrl} 仍是数据库中的永久文件定位信息，但不会直接出现在知识条目查询响应中；
 * 只有预览服务在确认文件可预览时，才会把它转换成对外的 {@code previewUrl}。</p>
 */
public record KnowledgeFilePreviewRow(
        String fileId,
        String entryId,
        String originalFileName,
        String fileType,
        String fileUrl) {
}
