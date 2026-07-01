package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeFilePreviewResponse;
import com.example.dqcadirsystem.knowledge.mapper.KnowledgeFileMapper;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeFilePreviewRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/** 文件预览能力判断和资源失效语义测试。 */
@ExtendWith(MockitoExtension.class)
class KnowledgeFilePreviewServiceTest {

    @Mock
    private KnowledgeFileMapper knowledgeFileMapper;

    @InjectMocks
    private KnowledgeFilePreviewService service;

    /** 当前正常 PDF 应返回真实预览地址。 */
    @Test
    void shouldReturnPdfPreviewUrl() {
        KnowledgeFilePreviewRow row = row("pdf");
        when(knowledgeFileMapper.selectCurrentPreviewById(2L)).thenReturn(row);

        KnowledgeFilePreviewResponse response = service.getPreview(2L);

        assertTrue(response.previewable());
        assertEquals("PDF", response.previewType());
        assertEquals(row.fileUrl(), response.previewUrl());
    }

    /** Word 和 CAD 等当前不支持在线预览的格式只返回元数据，不暴露 OSS 地址。 */
    @Test
    void shouldMarkNonPdfAsUnsupported() {
        for (String extension : new String[]{"doc", "docx", "dwg", "dxf"}) {
            KnowledgeFilePreviewRow row = row(extension);
            when(knowledgeFileMapper.selectCurrentPreviewById(2L)).thenReturn(row);

            KnowledgeFilePreviewResponse response = service.getPreview(2L);

            assertFalse(response.previewable());
            assertEquals("UNSUPPORTED", response.previewType());
            assertNull(response.previewUrl());
        }
    }

    /** 不存在、历史、删除或所属条目失效均由 Mapper 返回 null，并统一转换为 404。 */
    @Test
    void shouldRejectMissingOrInactiveFile() {
        when(knowledgeFileMapper.selectCurrentPreviewById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.getPreview(999L));

        assertEquals(CommonErrorCode.NOT_FOUND, exception.getErrorCode());
        assertEquals("文件不存在或已失效", exception.getMessage());
    }

    private KnowledgeFilePreviewRow row(String extension) {
        return new KnowledgeFilePreviewRow(
                "2", "1", "drawing." + extension, extension,
                "https://bucket/knowledge/1/2." + extension);
    }
}
