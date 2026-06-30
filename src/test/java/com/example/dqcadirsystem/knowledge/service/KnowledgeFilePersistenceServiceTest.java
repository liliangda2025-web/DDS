package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.knowledge.enums.KnowledgeFileType;
import com.example.dqcadirsystem.knowledge.mapper.KnowledgeFileMapper;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeFileRow;
import com.example.dqcadirsystem.knowledge.storage.StoredObject;
import com.example.dqcadirsystem.knowledge.validation.ValidatedKnowledgeFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 文件替换数据库事务的纯业务顺序测试。 */
@ExtendWith(MockitoExtension.class)
class KnowledgeFilePersistenceServiceTest {

    @Mock
    private KnowledgeFileMapper mapper;

    @InjectMocks
    private KnowledgeFilePersistenceService service;

    @Test
    void shouldLockDemoteInsertAndReadInOrder() {
        ValidatedKnowledgeFile file = new ValidatedKnowledgeFile("a.pdf", KnowledgeFileType.PDF, 10L);
        StoredObject stored = new StoredObject("knowledge/1/2.pdf", "https://bucket/knowledge/1/2.pdf");
        KnowledgeFileRow row = new KnowledgeFileRow(
                "2", "1", "a.pdf", "pdf", 10L, stored.publicUrl(), "success", 1,
                LocalDateTime.of(2026, 6, 30, 22, 0));
        when(mapper.selectActiveEntryIdForUpdate(1L)).thenReturn(1L);
        when(mapper.insertSuccessfulFile(2L, 1L, "a.pdf", "pdf", 10L, stored.publicUrl()))
                .thenReturn(1);
        when(mapper.selectById(2L)).thenReturn(row);

        assertEquals("2", service.replaceCurrentFile(1L, 2L, file, stored).fileId());

        var order = inOrder(mapper);
        order.verify(mapper).selectActiveEntryIdForUpdate(1L);
        order.verify(mapper).unsetCurrentFiles(1L);
        order.verify(mapper).insertSuccessfulFile(2L, 1L, "a.pdf", "pdf", 10L, stored.publicUrl());
        order.verify(mapper).selectById(2L);
    }

    /** 上传期间条目被删除时，事务内二次检查必须拒绝写文件。 */
    @Test
    void shouldRejectEntryDeletedDuringUpload() {
        ValidatedKnowledgeFile file = new ValidatedKnowledgeFile("a.pdf", KnowledgeFileType.PDF, 10L);
        StoredObject stored = new StoredObject("knowledge/1/2.pdf", "https://bucket/key");
        when(mapper.selectActiveEntryIdForUpdate(1L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.replaceCurrentFile(1L, 2L, file, stored));

        assertEquals(CommonErrorCode.NOT_FOUND, exception.getErrorCode());
        verify(mapper, never()).unsetCurrentFiles(1L);
    }
}
