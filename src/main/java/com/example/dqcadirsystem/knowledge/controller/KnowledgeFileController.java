package com.example.dqcadirsystem.knowledge.controller;

import com.example.dqcadirsystem.common.api.ApiResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeFileResponse;
import com.example.dqcadirsystem.knowledge.service.KnowledgeFileService;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识文件管理接口。
 *
 * <p>全局 context path 已统一配置为 {@code /api}，因此本类映射中不重复书写该前缀。</p>
 */
@RestController
@RequestMapping("/knowledge/entries/{entryId}/files")
@Validated
public class KnowledgeFileController {

    private final KnowledgeFileService knowledgeFileService;

    public KnowledgeFileController(KnowledgeFileService knowledgeFileService) {
        this.knowledgeFileService = knowledgeFileService;
    }

    /**
     * 上传或替换知识条目的当前文件。
     *
     * <p>表单字段固定为 {@code file}。文件校验、OSS 上传、数据库替换和失败补偿均由 Service 负责，
     * Controller 只负责协议绑定和统一响应包装。</p>
     */
    @PostMapping(value = "/current", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<KnowledgeFileResponse> uploadOrReplaceCurrentFile(
            @PathVariable @Positive(message = "知识条目ID必须大于0") Long entryId,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(
                "上传成功", knowledgeFileService.uploadOrReplaceCurrentFile(entryId, file));
    }
}
