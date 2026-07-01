package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.common.id.LongIdGenerator;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeFileResponse;
import com.example.dqcadirsystem.knowledge.enums.KnowledgeFileType;
import com.example.dqcadirsystem.knowledge.mapper.KnowledgeFileMapper;
import com.example.dqcadirsystem.knowledge.storage.StoredObject;
import com.example.dqcadirsystem.knowledge.validation.KnowledgeFileValidator;
import com.example.dqcadirsystem.knowledge.validation.ValidatedKnowledgeFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/** 文件上传跨 OSS 与数据库的编排和补偿测试。 */
@ExtendWith(MockitoExtension.class)
class KnowledgeFileServiceTest {

    @Mock
    private KnowledgeFileValidator validator;
    @Mock
    private KnowledgeFileMapper mapper;
    @Mock
    private LongIdGenerator idGenerator;
    @Mock
    private KnowledgeFileStorageCoordinator storageCoordinator;
    @Mock
    private KnowledgeFilePersistenceService persistenceService;

    private KnowledgeFileService service;
    private MockMultipartFile multipartFile;
    private ValidatedKnowledgeFile validatedFile;

    @BeforeEach
    void setUp() {
        service = new KnowledgeFileService(
                validator, mapper, idGenerator, storageCoordinator, persistenceService);
        multipartFile = new MockMultipartFile(
                "file", "drawing.pdf", "application/pdf", "%PDF-1.7".getBytes());
        validatedFile = new ValidatedKnowledgeFile("drawing.pdf", KnowledgeFileType.PDF, 8L);
    }

    /** 成功流程应先上传 OSS，再把存储结果交给事务服务。 */
    @Test
    void shouldUploadAndPersistCurrentFile() {
        long entryId = 2100000000000000001L;
        long fileId = 2200000000000000099L;
        StoredObject stored = new StoredObject(
                "knowledge/2100000000000000001/2200000000000000099.pdf",
                "https://bucket/knowledge/2100000000000000001/2200000000000000099.pdf");
        KnowledgeFileResponse expected = new KnowledgeFileResponse(
                Long.toString(fileId), Long.toString(entryId), "drawing.pdf", "pdf", 8L,
                stored.publicUrl(), "success", 1, LocalDateTime.now());
        when(validator.validate(multipartFile)).thenReturn(validatedFile);
        when(mapper.selectActiveEntryId(entryId)).thenReturn(entryId);
        when(idGenerator.nextId()).thenReturn(fileId);
        when(storageCoordinator.upload(entryId, fileId, multipartFile, validatedFile)).thenReturn(stored);
        when(persistenceService.replaceCurrentFile(entryId, fileId, validatedFile, stored))
                .thenReturn(expected);

        assertSame(expected, service.uploadOrReplaceCurrentFile(entryId, multipartFile));
        verify(storageCoordinator, never()).compensate(eq(stored), any());
    }

    /** 条目预检查失败时不得生成 ID 或产生 OSS 流量。 */
    @Test
    void shouldRejectMissingEntryBeforeUpload() {
        when(validator.validate(multipartFile)).thenReturn(validatedFile);
        when(mapper.selectActiveEntryId(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.uploadOrReplaceCurrentFile(999L, multipartFile));

        assertEquals(CommonErrorCode.NOT_FOUND, exception.getErrorCode());
        verify(idGenerator, never()).nextId();
        verify(storageCoordinator, never()).upload(any(), any(), any(), any());
    }

    /** 数据库事务失败时必须调用共享存储协调器执行补偿，并保留原始异常。 */
    @Test
    void shouldCompensateUploadedObjectWhenDatabaseFails() {
        StoredObject stored = new StoredObject("knowledge/1/2.pdf", "https://bucket/knowledge/1/2.pdf");
        RuntimeException databaseException = new RuntimeException("database failed");
        when(validator.validate(multipartFile)).thenReturn(validatedFile);
        when(mapper.selectActiveEntryId(1L)).thenReturn(1L);
        when(idGenerator.nextId()).thenReturn(2L);
        when(storageCoordinator.upload(1L, 2L, multipartFile, validatedFile)).thenReturn(stored);
        when(persistenceService.replaceCurrentFile(1L, 2L, validatedFile, stored))
                .thenThrow(databaseException);

        RuntimeException actual = assertThrows(
                RuntimeException.class, () -> service.uploadOrReplaceCurrentFile(1L, multipartFile));

        assertSame(databaseException, actual);
        verify(storageCoordinator).compensate(stored, databaseException);
    }
}
