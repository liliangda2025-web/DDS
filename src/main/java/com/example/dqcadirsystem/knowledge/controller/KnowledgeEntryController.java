package com.example.dqcadirsystem.knowledge.controller;

import com.example.dqcadirsystem.common.api.ApiResponse;
import com.example.dqcadirsystem.common.api.PageResponse;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryCreateRequest;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryPageRequest;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryCreateResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryDetailResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryPageItemResponse;
import com.example.dqcadirsystem.knowledge.service.KnowledgeEntryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识条目管理接口。
 *
 * <p>{@code /api} 由全局 context path 配置，本 Controller 只声明知识条目自身路径。</p>
 */
@RestController
@RequestMapping("/knowledge/entries")
@Validated
public class KnowledgeEntryController {

    private final KnowledgeEntryService knowledgeEntryService;

    public KnowledgeEntryController(KnowledgeEntryService knowledgeEntryService) {
        this.knowledgeEntryService = knowledgeEntryService;
    }

    /**
     * 分页查询知识条目及其当前有效文件信息。
     */
    @PostMapping("/page")
    public ApiResponse<PageResponse<KnowledgeEntryPageItemResponse>> pageEntries(
            @Valid @RequestBody KnowledgeEntryPageRequest request) {
        return ApiResponse.success(knowledgeEntryService.pageEntries(request));
    }

    /**
     * 按 ID 查看知识条目详情。
     *
     * <p>{@code /api} 由应用的 context path 统一添加。路径参数先通过 Bean Validation 校验为正数，
     * 再交给 Service 查询，避免无效 ID 进入数据库。</p>
     */
    @GetMapping("/{entryId}")
    public ApiResponse<KnowledgeEntryDetailResponse> getEntryDetail(
            @PathVariable @Positive(message = "知识条目ID必须大于0") Long entryId) {
        return ApiResponse.success(knowledgeEntryService.getEntryDetail(entryId));
    }

    /**
     * 手工新增知识条目元数据。
     *
     * <p>本接口不接收文件；新增成功后，调用方可使用返回的条目 ID 调用当前文件上传接口。
     * 请求体通过 Bean Validation 校验必填项和数据库字段长度。</p>
     */
    @PostMapping
    public ApiResponse<KnowledgeEntryCreateResponse> createEntry(
            @Valid @RequestBody KnowledgeEntryCreateRequest request) {
        return ApiResponse.success("新增成功", knowledgeEntryService.createEntry(request));
    }
}
