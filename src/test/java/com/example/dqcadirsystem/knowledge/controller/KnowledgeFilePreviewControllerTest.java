package com.example.dqcadirsystem.knowledge.controller;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.common.exception.GlobalExceptionHandler;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeFilePreviewResponse;
import com.example.dqcadirsystem.knowledge.service.KnowledgeFilePreviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 按 fileId 预览文件的 Web 层契约测试。 */
@WebMvcTest(KnowledgeFilePreviewController.class)
@Import(GlobalExceptionHandler.class)
class KnowledgeFilePreviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KnowledgeFilePreviewService previewService;

    /** PDF 响应必须使用 previewUrl，而不是再次暴露名为 fileUrl 的字段。 */
    @Test
    void shouldReturnPdfPreview() throws Exception {
        when(previewService.getPreview(2200000000000000001L)).thenReturn(
                new KnowledgeFilePreviewResponse(
                        "2200000000000000001", "2100000000000000001", "drawing.pdf", "pdf",
                        true, "PDF", "https://bucket/knowledge/entry/file.pdf"));

        mockMvc.perform(get("/api/knowledge/files/{fileId}/preview", 2200000000000000001L)
                        .contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.fileId").value("2200000000000000001"))
                .andExpect(jsonPath("$.data.previewable").value(true))
                .andExpect(jsonPath("$.data.previewType").value("PDF"))
                .andExpect(jsonPath("$.data.previewUrl").value("https://bucket/knowledge/entry/file.pdf"))
                .andExpect(jsonPath("$.data.fileUrl").doesNotExist());
    }

    /** 不可预览格式仍返回 200 和明确能力标记，previewUrl 固定为 null。 */
    @Test
    void shouldReturnUnsupportedPreviewCapability() throws Exception {
        when(previewService.getPreview(2L)).thenReturn(
                new KnowledgeFilePreviewResponse("2", "1", "drawing.dwg", "dwg",
                        false, "UNSUPPORTED", null));

        mockMvc.perform(get("/api/knowledge/files/2/preview").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previewable").value(false))
                .andExpect(jsonPath("$.data.previewType").value("UNSUPPORTED"))
                .andExpect(jsonPath("$.data.previewUrl").isEmpty());
    }

    /** 失效文件返回稳定的资源不存在语义。 */
    @Test
    void shouldReturnNotFoundForInactiveFile() throws Exception {
        when(previewService.getPreview(999L))
                .thenThrow(new BusinessException(CommonErrorCode.NOT_FOUND, "文件不存在或已失效"));

        mockMvc.perform(get("/api/knowledge/files/999/preview").contextPath("/api"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value("文件不存在或已失效"));
    }

    /** 非正数 fileId 在进入 Service 前即被拒绝。 */
    @Test
    void shouldRejectNonPositiveFileId() throws Exception {
        mockMvc.perform(get("/api/knowledge/files/0/preview").contextPath("/api"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("文件ID必须大于0"));
        verifyNoInteractions(previewService);
    }
}
