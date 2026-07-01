package com.example.dqcadirsystem.knowledge.controller;

import com.example.dqcadirsystem.common.api.ApiResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryInfoImportResponse;
import com.example.dqcadirsystem.knowledge.service.KnowledgeEntryInfoImportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 批量导入知识条目信息接口。
 *
 * <p>{@code /api} 由全局 context path 添加。Controller 只负责 multipart 绑定和统一响应包装，
 * Excel 结构校验、逐行处理及事务边界全部交给业务服务。</p>
 */
@RestController
@RequestMapping("/knowledge/entries")
public class KnowledgeEntryInfoImportController {

    private final KnowledgeEntryInfoImportService importService;

    public KnowledgeEntryInfoImportController(KnowledgeEntryInfoImportService importService) {
        this.importService = importService;
    }

    /** 上传填写完成的系统补充模板，并返回逐行导入汇总。 */
    @PostMapping(value = "/import-info", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<KnowledgeEntryInfoImportResponse> importInfo(
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success("导入完成", importService.importInfo(file));
    }
}
