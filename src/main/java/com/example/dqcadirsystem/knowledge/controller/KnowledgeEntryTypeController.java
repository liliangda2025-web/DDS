package com.example.dqcadirsystem.knowledge.controller;

import com.example.dqcadirsystem.common.api.ApiResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryTypeResponse;
import com.example.dqcadirsystem.knowledge.service.KnowledgeEntryTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识条目类型接口。
 *
 * <p>Controller 只处理请求路径和统一响应包装，具体的数据组装交给服务层。当前接口是只读接口，
 * 不接收参数，也不会访问数据库。</p>
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeEntryTypeController {

    private final KnowledgeEntryTypeService knowledgeEntryTypeService;

    /**
     * 使用构造器注入依赖，保证 Controller 创建后所需服务一定存在，同时便于测试替换依赖。
     */
    public KnowledgeEntryTypeController(KnowledgeEntryTypeService knowledgeEntryTypeService) {
        this.knowledgeEntryTypeService = knowledgeEntryTypeService;
    }

    /**
     * 获取系统当前支持的知识条目类型列表。
     *
     * <p>Spring MVC 默认对正常返回使用 HTTP 200；{@link ApiResponse#success(Object)} 再把响应体业务码
     * 设置为 200，并使用统一提示“操作成功”。</p>
     *
     * @return 包含四种固定知识条目类型的统一响应
     */
    @GetMapping("/entry-types")
    public ApiResponse<List<KnowledgeEntryTypeResponse>> getEntryTypes() {
        return ApiResponse.success(knowledgeEntryTypeService.listEntryTypes());
    }
}
