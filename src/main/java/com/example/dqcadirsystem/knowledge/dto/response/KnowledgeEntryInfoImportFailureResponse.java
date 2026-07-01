package com.example.dqcadirsystem.knowledge.dto.response;

import com.example.dqcadirsystem.knowledge.service.model.KnowledgeEntryInfoImportRow;

/** 批量导入知识条目信息的一条失败明细。 */
public record KnowledgeEntryInfoImportFailureResponse(
        int rowNum,
        String entryId,
        String fileId,
        String fileName,
        String reason) {

    /** 使用 Excel 原始定位信息构造结果，即使 ID 无法转换为 Long 也能帮助用户定位问题行。 */
    public static KnowledgeEntryInfoImportFailureResponse from(
            KnowledgeEntryInfoImportRow row, String reason) {
        return new KnowledgeEntryInfoImportFailureResponse(
                row.rowNum(), row.entryIdText(), row.fileIdText(), row.fileName(), reason);
    }
}
