package com.example.dqcadirsystem.knowledge.controller;

import com.example.dqcadirsystem.common.api.ApiResponse;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeSearchRequest;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeSearchResponse;
import com.example.dqcadirsystem.knowledge.service.KnowledgeSearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 知识检索与推荐页面接口；{@code /api} 由全局 context path 统一添加。 */
@RestController
@RequestMapping("/knowledge")
public class KnowledgeSearchController {

    private final KnowledgeSearchService knowledgeSearchService;

    public KnowledgeSearchController(KnowledgeSearchService knowledgeSearchService) {
        this.knowledgeSearchService = knowledgeSearchService;
    }

    /** 返回当前页的有效知识文件卡片，不返回文件地址、评分或高亮信息。 */
    @PostMapping("/search")
    public ApiResponse<KnowledgeSearchResponse> search(
            @Valid @RequestBody KnowledgeSearchRequest request) {
        return ApiResponse.success("检索成功", knowledgeSearchService.search(request));
    }
}
