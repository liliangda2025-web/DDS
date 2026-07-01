package com.example.dqcadirsystem.knowledge.service;

/**
 * 已在内存中完整生成的 Excel 导出文件。
 *
 * @param filename 响应下载文件名
 * @param content 完整的 XLSX 二进制内容
 */
public record KnowledgeSupplementTemplateExportFile(String filename, byte[] content) {

    /** 防止外部持有并修改生成组件传入的原始数组。 */
    public KnowledgeSupplementTemplateExportFile {
        content = content.clone();
    }

    /** 调用方取得副本，避免 HTTP 适配层之外的代码修改已生成文件。 */
    @Override
    public byte[] content() {
        return content.clone();
    }
}
