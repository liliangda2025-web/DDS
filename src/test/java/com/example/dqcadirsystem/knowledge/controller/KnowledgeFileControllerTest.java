package com.example.dqcadirsystem.knowledge.controller;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.common.exception.GlobalExceptionHandler;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeFileResponse;
import com.example.dqcadirsystem.knowledge.service.KnowledgeFileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 知识文件上传接口的 Web 契约测试。 */
@WebMvcTest(KnowledgeFileController.class)
@Import(GlobalExceptionHandler.class)
class KnowledgeFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KnowledgeFileService knowledgeFileService;

    /** 验证完整外部路径、multipart 字段和统一响应中的全部文件字段。 */
    @Test
    void shouldUploadCurrentFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "暖通图纸.pdf", "application/pdf", "%PDF-1.7".getBytes());
        KnowledgeFileResponse response = new KnowledgeFileResponse(
                "2200000000000000099", "2100000000000000001", "暖通图纸.pdf", "pdf", 8L,
                "https://liliangda-oss-test.oss-cn-beijing.aliyuncs.com/knowledge/2100000000000000001/2200000000000000099.pdf",
                "success", 1, LocalDateTime.of(2026, 6, 30, 22, 0));
        when(knowledgeFileService.uploadOrReplaceCurrentFile(eq(2100000000000000001L), any()))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/knowledge/entries/{entryId}/files/current", 2100000000000000001L)
                        .file(file)
                        .contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("上传成功"))
                .andExpect(jsonPath("$.data.fileId").value("2200000000000000099"))
                .andExpect(jsonPath("$.data.fileType").value("pdf"))
                .andExpect(jsonPath("$.data.uploadStatus").value("success"))
                .andExpect(jsonPath("$.data.isCurrent").value(1))
                .andExpect(jsonPath("$.data.uploadedAt").value("2026-06-30 22:00:00"));
    }

    /** 缺少约定的 file 表单字段时仍返回统一参数错误。 */
    @Test
    void shouldRejectMissingFilePart() throws Exception {
        mockMvc.perform(multipart("/api/knowledge/entries/{entryId}/files/current", 1L)
                        .contextPath("/api"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("缺少必填文件: file"));
    }

    /** Service 的条目不存在语义应由全局异常处理器转换为 404。 */
    @Test
    void shouldReturnNotFoundWhenEntryDoesNotExist() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "%PDF-".getBytes());
        when(knowledgeFileService.uploadOrReplaceCurrentFile(eq(999L), any()))
                .thenThrow(new BusinessException(CommonErrorCode.NOT_FOUND, "知识条目不存在"));

        mockMvc.perform(multipart("/api/knowledge/entries/{entryId}/files/current", 999L)
                        .file(file)
                        .contextPath("/api"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value("知识条目不存在"));
    }
}
