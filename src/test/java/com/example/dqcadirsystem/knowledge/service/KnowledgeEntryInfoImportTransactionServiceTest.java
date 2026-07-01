package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryUpdateRequest;
import com.example.dqcadirsystem.knowledge.mapper.KnowledgeEntryMapper;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeEntryInfoImportFileRow;
import com.example.dqcadirsystem.knowledge.service.model.KnowledgeEntryInfoImportRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证单行事务在写入前重新核对条目、文件和业务唯一键。 */
@ExtendWith(MockitoExtension.class)
class KnowledgeEntryInfoImportTransactionServiceTest {

    @Mock
    private KnowledgeEntryMapper mapper;

    private KnowledgeEntryInfoImportTransactionService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeEntryInfoImportTransactionService(mapper);
    }

    @Test
    void shouldUpdatePendingEntryWhenSystemColumnsMatch() {
        KnowledgeEntryInfoImportRow row = row();
        when(mapper.selectActiveInfoStatusForUpdate(row.entryId())).thenReturn(0);
        when(mapper.selectValidImportFile(row.entryId(), row.fileId())).thenReturn(file());
        when(mapper.countActiveByBusinessKey(any(), org.mockito.ArgumentMatchers.eq(row.entryId())))
                .thenReturn(0);
        when(mapper.updateEntry(org.mockito.ArgumentMatchers.eq(row.entryId()), any())).thenReturn(1);

        service.importRow(row);

        verify(mapper).updateEntry(org.mockito.ArgumentMatchers.eq(row.entryId()), any(KnowledgeEntryUpdateRequest.class));
    }

    @Test
    void shouldRejectAlreadyCompletedEntryBeforeFileQuery() {
        KnowledgeEntryInfoImportRow row = row();
        when(mapper.selectActiveInfoStatusForUpdate(row.entryId())).thenReturn(1);

        KnowledgeEntryInfoImportRowException exception = assertThrows(
                KnowledgeEntryInfoImportRowException.class, () -> service.importRow(row));

        assertEquals("知识条目已完善", exception.getMessage());
        verify(mapper, never()).selectValidImportFile(row.entryId(), row.fileId());
    }

    @Test
    void shouldRejectModifiedSystemColumns() {
        KnowledgeEntryInfoImportRow row = row();
        KnowledgeEntryInfoImportFileRow changed = new KnowledgeEntryInfoImportFileRow(
                row.fileId(), row.entryId(), "changed.pdf", "pdf", 100L,
                row.uploadedAt(), "success");
        when(mapper.selectActiveInfoStatusForUpdate(row.entryId())).thenReturn(0);
        when(mapper.selectValidImportFile(row.entryId(), row.fileId())).thenReturn(changed);

        KnowledgeEntryInfoImportRowException exception = assertThrows(
                KnowledgeEntryInfoImportRowException.class, () -> service.importRow(row));

        assertEquals("系统识别字段已被修改，请重新导出模板", exception.getMessage());
        verify(mapper, never()).updateEntry(org.mockito.ArgumentMatchers.anyLong(), any());
    }

    private KnowledgeEntryInfoImportRow row() {
        return new KnowledgeEntryInfoImportRow(
                2, "2100000000000000010", 2100000000000000010L,
                "2200000000000000010", 2200000000000000010L,
                "file.pdf", "pdf", 100L, LocalDateTime.of(2026, 7, 1, 10, 0), "success",
                "DRAWING", "DWG-001", "正式标题", null, "V1.0", null,
                LocalDate.of(2026, 7, 1), null, null, null, null, null);
    }

    private KnowledgeEntryInfoImportFileRow file() {
        return new KnowledgeEntryInfoImportFileRow(
                2200000000000000010L, 2100000000000000010L, "file.pdf", "pdf", 100L,
                LocalDateTime.of(2026, 7, 1, 10, 0), "success");
    }
}
