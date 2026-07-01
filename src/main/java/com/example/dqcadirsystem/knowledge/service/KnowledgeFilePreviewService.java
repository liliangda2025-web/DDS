package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeFilePreviewResponse;
import com.example.dqcadirsystem.knowledge.mapper.KnowledgeFileMapper;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeFilePreviewRow;
import org.springframework.stereotype.Service;

/**
 * 根据文件 ID 构建统一预览信息。
 *
 * <p>服务不对 OSS 发送 HEAD 请求。数据库负责判断文件与条目是否仍然有效，浏览器加载地址失败时，
 * 前端按照统一的“文件预览失败”交互处理。后续切换私有 Bucket 后，可在本服务中集中生成签名 URL，
 * 而无需重新修改列表、详情和检索接口。</p>
 */
@Service
public class KnowledgeFilePreviewService {

    private static final String PDF_EXTENSION = "pdf";
    private static final String PDF_PREVIEW_TYPE = "PDF";
    private static final String UNSUPPORTED_PREVIEW_TYPE = "UNSUPPORTED";

    private final KnowledgeFileMapper knowledgeFileMapper;

    public KnowledgeFilePreviewService(KnowledgeFileMapper knowledgeFileMapper) {
        this.knowledgeFileMapper = knowledgeFileMapper;
    }

    /**
     * 查询当前正常文件，并根据扩展名确定当前版本是否提供在线预览。
     */
    public KnowledgeFilePreviewResponse getPreview(Long fileId) {
        KnowledgeFilePreviewRow row = knowledgeFileMapper.selectCurrentPreviewById(fileId);
        if (row == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "文件不存在或已失效");
        }

        boolean previewable = PDF_EXTENSION.equalsIgnoreCase(row.fileType());
        return new KnowledgeFilePreviewResponse(
                row.fileId(),
                row.entryId(),
                row.originalFileName(),
                row.fileType(),
                previewable,
                previewable ? PDF_PREVIEW_TYPE : UNSUPPORTED_PREVIEW_TYPE,
                previewable ? row.fileUrl() : null);
    }
}
