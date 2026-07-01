package com.example.dqcadirsystem.knowledge.controller;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.common.exception.GlobalExceptionHandler;
import com.example.dqcadirsystem.knowledge.service.KnowledgeSupplementTemplateExportFile;
import com.example.dqcadirsystem.knowledge.service.KnowledgeSupplementTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 补充模板二进制成功响应和统一 JSON 失败响应测试。 */
@WebMvcTest(KnowledgeSupplementTemplateController.class)
@Import(GlobalExceptionHandler.class)
class KnowledgeSupplementTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KnowledgeSupplementTemplateService templateService;

    @Test
    void shouldReturnCompleteXlsxWithDownloadHeaders() throws Exception {
        byte[] bytes = {0x50, 0x4B, 0x03, 0x04};
        when(templateService.export(any())).thenReturn(new KnowledgeSupplementTemplateExportFile(
                "knowledge_supplement_template_20260701103000.xlsx", bytes));

        mockMvc.perform(post("/api/knowledge/entries/supplement-template/export")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(bytes))
                .andExpect(header().longValue("Content-Length", bytes.length))
                .andExpect(header().string("Content-Disposition",
                        containsString("knowledge_supplement_template_20260701103000.xlsx")))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    @Test
    void shouldReturnUnifiedJsonWhenBusinessValidationFails() throws Exception {
        when(templateService.export(any())).thenThrow(
                new BusinessException(CommonErrorCode.BAD_REQUEST, "没有可导出的待补充记录"));

        mockMvc.perform(post("/api/knowledge/entries/supplement-template/export")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("没有可导出的待补充记录"))
                .andExpect(jsonPath("$.data").value((Object) null));
    }

    @Test
    void shouldValidateEntryIdBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/knowledge/entries/supplement-template/export")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entryIds\":[\"0\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("知识条目ID必须大于0"));
    }

    /** 工作簿生成故障必须返回安全的系统错误 JSON，不能泄露 POI 内部信息或部分文件。 */
    @Test
    void shouldHideWorkbookGenerationFailure() throws Exception {
        when(templateService.export(any())).thenThrow(
                new IllegalStateException("sensitive workbook implementation detail"));

        mockMvc.perform(post("/api/knowledge/entries/supplement-template/export")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(50000))
                .andExpect(jsonPath("$.message").value("系统繁忙，请稍后重试"));
    }
}
