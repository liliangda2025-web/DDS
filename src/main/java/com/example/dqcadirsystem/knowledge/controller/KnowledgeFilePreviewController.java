package com.example.dqcadirsystem.knowledge.controller;

import com.example.dqcadirsystem.common.api.ApiResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeFilePreviewResponse;
import com.example.dqcadirsystem.knowledge.service.KnowledgeFilePreviewService;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 以文件 ID 为唯一入口的文件预览接口。
 *
 * <p>{@code /api} 由应用 context path 统一添加，本 Controller 只声明知识库内部路径。</p>
 */
@RestController
@RequestMapping("/knowledge/files")
@Validated
public class KnowledgeFilePreviewController {

    private final KnowledgeFilePreviewService knowledgeFilePreviewService;

    public KnowledgeFilePreviewController(KnowledgeFilePreviewService knowledgeFilePreviewService) {
        this.knowledgeFilePreviewService = knowledgeFilePreviewService;
    }

    /**
     * 返回指定当前文件的预览能力和预览地址。
     */
    @GetMapping("/{fileId}/preview")
    public ApiResponse<KnowledgeFilePreviewResponse> getPreview(
            @PathVariable @Positive(message = "文件ID必须大于0") Long fileId) {
        return ApiResponse.success(knowledgeFilePreviewService.getPreview(fileId));
    }
}
