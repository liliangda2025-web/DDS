package com.example.dqcadirsystem.knowledge.controller;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.common.exception.GlobalExceptionHandler;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeBatchUploadFileResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeBatchUploadResponse;
import com.example.dqcadirsystem.knowledge.service.KnowledgeBatchUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 批量导入文件接口的 multipart 绑定和统一响应契约测试。 */
@WebMvcTest(KnowledgeBatchUploadController.class)
@Import(GlobalExceptionHandler.class)
class KnowledgeBatchUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KnowledgeBatchUploadService batchUploadService;

    @Test
    void shouldReturnCompleteMixedBatchResult() throws Exception {
        MockMultipartFile entryType = new MockMultipartFile(
                "entryType", "", "text/plain", "DRAWING".getBytes());
        MockMultipartFile first = new MockMultipartFile(
                "files", "a.pdf", "application/pdf", "%PDF-1.7".getBytes());
        MockMultipartFile second = new MockMultipartFile(
                "files", "bad.exe", "application/octet-stream", "bad".getBytes());
        KnowledgeBatchUploadResponse response = new KnowledgeBatchUploadResponse(
                2,
                1,
                1,
                List.of(
                        KnowledgeBatchUploadFileResponse.success(
                                1, "11", "12", "a.pdf", "pdf", 8L,
                                "https://bucket/knowledge/11/12.pdf",
                                LocalDateTime.of(2026, 7, 1, 10, 0)),
                        KnowledgeBatchUploadFileResponse.failed(
                                2, "bad.exe", "exe", 3L, "文件格式不支持")));
        when(batchUploadService.upload(eq("DRAWING"), anyList())).thenReturn(response);

        mockMvc.perform(multipart("/api/knowledge/files/batch-upload")
                        .file(entryType)
                        .file(first)
                        .file(second)
                        .contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("批量上传完成"))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.fileList[0].uploadStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.fileList[0].uploadedAt").value("2026-07-01 10:00:00"))
                .andExpect(jsonPath("$.data.fileList[1].uploadStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.fileList[1].entryId").value((Object) null))
                .andExpect(jsonPath("$.data.fileList[1].errorMsg").value("文件格式不支持"));
    }

    @Test
    void shouldRejectMissingFilesPart() throws Exception {
        MockMultipartFile entryType = new MockMultipartFile(
                "entryType", "", "text/plain", "DRAWING".getBytes());

        mockMvc.perform(multipart("/api/knowledge/files/batch-upload")
                        .file(entryType)
                        .contextPath("/api"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("缺少必填文件: files"));
    }

    @Test
    void shouldRejectMissingEntryTypePart() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "a.pdf", "application/pdf", "%PDF-1.7".getBytes());

        mockMvc.perform(multipart("/api/knowledge/files/batch-upload")
                        .file(file)
                        .contextPath("/api"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("缺少必填参数: entryType"));
    }

    @Test
    void shouldReturnRequestLevelBusinessValidationError() throws Exception {
        MockMultipartFile entryType = new MockMultipartFile(
                "entryType", "", "text/plain", "DRAWING".getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "files", "a.pdf", "application/pdf", "%PDF-1.7".getBytes());
        when(batchUploadService.upload(eq("DRAWING"), anyList())).thenThrow(
                new BusinessException(CommonErrorCode.BAD_REQUEST, "单次批量上传不能超过10个文件"));

        mockMvc.perform(multipart("/api/knowledge/files/batch-upload")
                        .file(entryType)
                        .file(file)
                        .contextPath("/api"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("单次批量上传不能超过10个文件"));
    }
}
