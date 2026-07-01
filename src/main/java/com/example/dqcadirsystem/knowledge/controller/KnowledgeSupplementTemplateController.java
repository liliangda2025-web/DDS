package com.example.dqcadirsystem.knowledge.controller;

import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeSupplementTemplateExportRequest;
import com.example.dqcadirsystem.knowledge.service.KnowledgeSupplementTemplateExportFile;
import com.example.dqcadirsystem.knowledge.service.KnowledgeSupplementTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 知识条目信息补充模板导出接口。
 *
 * <p>成功响应是 Excel 二进制，不能使用统一 JSON 包装；业务或系统异常仍由全局异常处理器转换为
 * {@code code/message/data} JSON。Controller 不声明固定 {@code produces}，确保异常响应可以正确协商 JSON。</p>
 */
@RestController
@RequestMapping("/knowledge/entries/supplement-template")
public class KnowledgeSupplementTemplateController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final KnowledgeSupplementTemplateService templateService;

    public KnowledgeSupplementTemplateController(KnowledgeSupplementTemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * 完整生成模板后再设置响应头和响应体，保证生成中途失败时不会向客户端发送残缺 Excel。
     */
    @PostMapping(value = "/export", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> export(@Valid @RequestBody KnowledgeSupplementTemplateExportRequest request) {
        KnowledgeSupplementTemplateExportFile file = templateService.export(request);
        byte[] content = file.content();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .contentLength(content.length)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(content);
    }
}
