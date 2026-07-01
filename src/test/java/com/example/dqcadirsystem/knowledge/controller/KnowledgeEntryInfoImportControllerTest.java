package com.example.dqcadirsystem.knowledge.controller;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.common.exception.GlobalExceptionHandler;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryInfoImportFailureResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryInfoImportResponse;
import com.example.dqcadirsystem.knowledge.service.KnowledgeEntryInfoImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 批量知识信息导入的 multipart 绑定、部分成功和统一异常响应测试。 */
@WebMvcTest(KnowledgeEntryInfoImportController.class)
@Import(GlobalExceptionHandler.class)
class KnowledgeEntryInfoImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KnowledgeEntryInfoImportService importService;

    @Test
    void shouldReturnPartialImportResult() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1});
        when(importService.importInfo(any())).thenReturn(new KnowledgeEntryInfoImportResponse(
                2, 1, 1, List.of(new KnowledgeEntryInfoImportFailureResponse(
                3, "11", "21", "bad.pdf", "标题不能为空"))));

        mockMvc.perform(multipart("/api/knowledge/entries/import-info")
                        .file(file).contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("导入完成"))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.failedList[0].rowNum").value(3))
                .andExpect(jsonPath("$.data.failedList[0].reason").value("标题不能为空"));
    }

    @Test
    void shouldRejectMissingFilePart() throws Exception {
        mockMvc.perform(multipart("/api/knowledge/entries/import-info").contextPath("/api"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("缺少必填文件: file"));
    }

    @Test
    void shouldReturnFileLevelValidationAsJson() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "bad.xlsx", null, new byte[]{1});
        when(importService.importInfo(any())).thenThrow(
                new BusinessException(CommonErrorCode.BAD_REQUEST, "Excel表头不正确"));

        mockMvc.perform(multipart("/api/knowledge/entries/import-info")
                        .file(file).contextPath("/api"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("Excel表头不正确"))
                .andExpect(jsonPath("$.data").value((Object) null));
    }
}
