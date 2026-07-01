package com.example.dqcadirsystem.knowledge.dto.response;

/**
 * 文件预览入口的响应数据。
 *
 * <p>当前版本只具备 PDF 在线预览能力。其他受支持的上传格式仍返回文件元数据，但明确标记为不可预览，
 * 且不会返回底层 OSS 地址，防止前端绕过统一预览策略。</p>
 *
 * @param fileId 文件 ID，使用字符串避免 JavaScript 大整数精度损失
 * @param entryId 所属知识条目 ID
 * @param originalFileName 上传时的原始文件名
 * @param fileType 标准化小写扩展名
 * @param previewable 当前版本是否支持在线预览
 * @param previewType 预览类型：PDF 或 UNSUPPORTED
 * @param previewUrl 可预览时的地址；不可预览时固定为 null
 */
public record KnowledgeFilePreviewResponse(
        String fileId,
        String entryId,
        String originalFileName,
        String fileType,
        boolean previewable,
        String previewType,
        String previewUrl) {
}
