package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeSupplementTemplateExportRequest;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeSupplementTemplateRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 补充模板导出模式、顺序、上限和幂等只读语义测试。 */
@ExtendWith(MockitoExtension.class)
class KnowledgeSupplementTemplateServiceTest {

    @Mock
    private KnowledgeSupplementTemplateQueryService queryService;
    @Mock
    private KnowledgeSupplementTemplateExcelWriter excelWriter;

    private KnowledgeSupplementTemplateService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeSupplementTemplateService(queryService, excelWriter);
    }

    @Test
    void shouldPreserveRequestedOrderInExactMode() {
        KnowledgeSupplementTemplateExportRequest request = request(List.of(2L, 1L));
        KnowledgeSupplementTemplateRow first = row(1L);
        KnowledgeSupplementTemplateRow second = row(2L);
        when(queryService.queryExact(request.entryIds())).thenReturn(List.of(first, second));
        when(excelWriter.write(List.of(second, first))).thenReturn(new byte[]{1, 2, 3});

        KnowledgeSupplementTemplateExportFile file = service.export(request);

        assertEquals(3, file.content().length);
        assertTrue(file.filename().matches("knowledge_supplement_template_\\d{14}\\.xlsx"));
        verify(excelWriter).write(List.of(second, first));
    }

    @Test
    void shouldRejectMixedModesDuplicateIdsAndInvalidTime() {
        assertMessage("entryIds不能与筛选条件同时使用", new KnowledgeSupplementTemplateExportRequest(
                List.of(1L), "DRAWING", null, null, null));
        assertMessage("知识条目ID不能重复", request(List.of(1L, 1L)));
        assertMessage("知识条目类型不正确", new KnowledgeSupplementTemplateExportRequest(
                null, "drawing", null, null, null));
        assertMessage("上传开始时间不能晚于结束时间", new KnowledgeSupplementTemplateExportRequest(
                null, null, null,
                LocalDateTime.of(2026, 7, 2, 0, 0),
                LocalDateTime.of(2026, 7, 1, 0, 0)));
        verify(queryService, never()).queryExact(any());
        verify(queryService, never()).queryByFilter(any(), anyInt());
    }

    @Test
    void shouldRejectIncompleteExactResultAndEmptyFilterResult() {
        KnowledgeSupplementTemplateExportRequest exact = request(List.of(1L, 2L));
        when(queryService.queryExact(exact.entryIds())).thenReturn(List.of(row(1L)));
        assertMessage("部分知识条目不存在、已完善或没有有效文件", exact);

        KnowledgeSupplementTemplateExportRequest filter = request(null);
        when(queryService.queryByFilter(filter, 5001)).thenReturn(List.of());
        assertMessage("没有可导出的待补充记录", filter);
        verify(excelWriter, never()).write(any());
    }

    @Test
    void shouldRejectMoreThanFiveThousandFilteredRows() {
        KnowledgeSupplementTemplateExportRequest filter = request(null);
        when(queryService.queryByFilter(filter, 5001))
                .thenReturn(Collections.nCopies(5001, row(1L)));

        assertMessage("可导出记录超过5000条，请缩小筛选范围", filter);
        verify(excelWriter, never()).write(any());
    }

    /** 服务依赖只有只读查询和内存生成器，因此重复导出不会改变待补充状态。 */
    @Test
    void shouldAllowRetryWithSamePendingRows() {
        KnowledgeSupplementTemplateExportRequest filter = request(null);
        List<KnowledgeSupplementTemplateRow> rows = List.of(row(1L));
        when(queryService.queryByFilter(filter, 5001)).thenReturn(rows);
        when(excelWriter.write(rows)).thenReturn(new byte[]{1});

        service.export(filter);
        service.export(filter);

        verify(queryService, org.mockito.Mockito.times(2)).queryByFilter(filter, 5001);
        verify(excelWriter, org.mockito.Mockito.times(2)).write(rows);
    }

    private void assertMessage(String message, KnowledgeSupplementTemplateExportRequest request) {
        BusinessException exception = assertThrows(BusinessException.class, () -> service.export(request));
        assertEquals(message, exception.getMessage());
    }

    private KnowledgeSupplementTemplateExportRequest request(List<Long> ids) {
        return new KnowledgeSupplementTemplateExportRequest(ids, null, null, null, null);
    }

    private KnowledgeSupplementTemplateRow row(long entryId) {
        return new KnowledgeSupplementTemplateRow(
                Long.toString(entryId), Long.toString(entryId + 100), "图纸.pdf", "pdf", 100L,
                LocalDateTime.of(2026, 7, 1, 10, 0), "success", "DRAWING",
                "TMP_" + entryId, "图纸", "待补充", "TEMP", null,
                LocalDate.of(2026, 7, 1), "图纸库", null, null, null);
    }
}
