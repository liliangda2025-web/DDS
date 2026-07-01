package com.example.dqcadirsystem.knowledge.mapper.model;

import java.time.LocalDateTime;

/**
 * 批量信息导入时用于核对系统识别列的文件快照。
 *
 * <p>查询只会返回正常、当前且上传成功的文件，因此该对象存在本身就说明文件关系和状态有效。
 * 其余字段用于检测用户是否绕过 Excel 工作表保护修改了系统列。</p>
 */
public record KnowledgeEntryInfoImportFileRow(
        Long fileId,
        Long entryId,
        String originalFileName,
        String fileType,
        Long fileSize,
        LocalDateTime uploadedAt,
        String uploadStatus) {
}
