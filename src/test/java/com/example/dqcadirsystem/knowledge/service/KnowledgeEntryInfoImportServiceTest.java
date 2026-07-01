package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryInfoImportResponse;
import com.example.dqcadirsystem.knowledge.service.model.KnowledgeEntryInfoImportRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证整表预检查、部分成功和未知单行异常的安全降级。 */
@ExtendWith(MockitoExtension.class)
class KnowledgeEntryInfoImportServiceTest {

    @Mock
    private KnowledgeEntryInfoExcelParser parser;

    @Mock
    private KnowledgeEntryInfoImportTransactionService transactionService;

    private KnowledgeEntryInfoImportService service;
    private final MockMultipartFile file = new MockMultipartFile("file", "a.xlsx", null, new byte[]{1});

    @BeforeEach
    void setUp() {
        service = new KnowledgeEntryInfoImportService(parser, transactionService);
    }

    @Test
    void shouldContinueAfterExpectedRowFailure() {
        KnowledgeEntryInfoImportRow first = row(2, 10L, 20L, "DWG-001");
        KnowledgeEntryInfoImportRow second = row(3, 11L, 21L, "DWG-002");
        when(parser.parse(file)).thenReturn(List.of(first, second));
        doNothing().when(transactionService).importRow(first);
        doThrow(new KnowledgeEntryInfoImportRowException("标题不能为空"))
                .when(transactionService).importRow(second);

        KnowledgeEntryInfoImportResponse response = service.importInfo(file);

        assertEquals(List.of("标题不能为空"),
                response.failedList().stream().map(failure -> failure.reason()).toList());
        assertEquals(2, response.totalCount());
        assertEquals(1, response.successCount());
        assertEquals(1, response.failedCount());
        assertEquals(3, response.failedList().getFirst().rowNum());
        assertEquals("标题不能为空", response.failedList().getFirst().reason());
    }

    @Test
    void shouldFailAllRowsWithDuplicateEntryIdBeforeDatabaseProcessing() {
        KnowledgeEntryInfoImportRow first = row(2, 10L, 20L, "DWG-001");
        KnowledgeEntryInfoImportRow second = row(3, 10L, 21L, "DWG-002");
        when(parser.parse(file)).thenReturn(List.of(first, second));

        KnowledgeEntryInfoImportResponse response = service.importInfo(file);

        assertEquals(0, response.successCount());
        assertEquals(2, response.failedCount());
        assertEquals("knowledge_entry_id在Excel中重复", response.failedList().getFirst().reason());
        verify(transactionService, never()).importRow(first);
        verify(transactionService, never()).importRow(second);
    }

    @Test
    void shouldConvertUnexpectedRowFailureAndContinue() {
        KnowledgeEntryInfoImportRow first = row(2, 10L, 20L, "DWG-001");
        KnowledgeEntryInfoImportRow second = row(3, 11L, 21L, "DWG-002");
        when(parser.parse(file)).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("sensitive SQL detail"))
                .when(transactionService).importRow(first);
        doNothing().when(transactionService).importRow(second);

        KnowledgeEntryInfoImportResponse response = service.importInfo(file);

        assertEquals(1, response.successCount());
        assertEquals("该行处理失败，请稍后重试", response.failedList().getFirst().reason());
        verify(transactionService).importRow(second);
    }

    private KnowledgeEntryInfoImportRow row(int rowNum, long entryId, long fileId, String entryCode) {
        return new KnowledgeEntryInfoImportRow(
                rowNum, Long.toString(entryId), entryId, Long.toString(fileId), fileId,
                "file.pdf", "pdf", 100L, LocalDateTime.of(2026, 7, 1, 10, 0), "success",
                "DRAWING", entryCode, "正式标题", null, "V1.0", null,
                LocalDate.of(2026, 7, 1), null, null, null, null, null);
    }
}
