package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeSearchRequest;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeSearchResponse;
import com.example.dqcadirsystem.knowledge.mapper.KnowledgeEntryMapper;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeSearchRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 知识检索分页、类型转换、空结果和参数业务校验测试。 */
@ExtendWith(MockitoExtension.class)
class KnowledgeSearchServiceTest {

    @Mock
    private KnowledgeEntryMapper mapper;

    private KnowledgeSearchService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeSearchService(mapper);
    }

    @Test
    void shouldReturnSearchCardsAndPageMetadata() {
        KnowledgeSearchRequest request = new KnowledgeSearchRequest("暖通", "DRAWING", 2, 10);
        when(mapper.countSearch(request)).thenReturn(35L);
        when(mapper.selectSearch(request)).thenReturn(List.of(row()));

        KnowledgeSearchResponse response = service.search(request);

        assertEquals(35, response.total());
        assertEquals(4, response.pages());
        assertEquals(2, response.pageNum());
        assertEquals("图纸库", response.records().getFirst().entryTypeName());
        assertEquals("暖通图纸", response.records().getFirst().displayName());
        assertTrue(response.messageText().startsWith("共找到35份相关图纸，耗时"));
        assertEquals(2, response.costSeconds().scale());
    }

    @Test
    void shouldReturnFriendlyEmptyResultWithoutPageQuery() {
        KnowledgeSearchRequest request = new KnowledgeSearchRequest("不存在", null, null, null);
        when(mapper.countSearch(request)).thenReturn(0L);

        KnowledgeSearchResponse response = service.search(request);

        assertEquals(0, response.total());
        assertEquals(0, response.pages());
        assertTrue(response.records().isEmpty());
        assertEquals("查无结果，请调整关键词后重试", response.messageText());
        verify(mapper, never()).selectSearch(request);
    }

    @Test
    void shouldRejectUnknownEntryTypeBeforeQueryingDatabase() {
        KnowledgeSearchRequest request = new KnowledgeSearchRequest("暖通", "UNKNOWN", 1, 10);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.search(request));

        assertEquals("知识条目类型不合法", exception.getMessage());
        verify(mapper, never()).countSearch(request);
    }

    private KnowledgeSearchRow row() {
        return new KnowledgeSearchRow(
                "2100000000000000001", "2200000000000000001", "DRAWING", "DWG-001",
                "暖通图纸", "暖通 空调", "总部项目", "图纸库", "HVAC", "张工",
                "暖通图纸.pdf", "pdf");
    }
}
