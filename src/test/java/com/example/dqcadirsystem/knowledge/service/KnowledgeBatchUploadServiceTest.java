package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.common.id.LongIdGenerator;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeBatchUploadResponse;
import com.example.dqcadirsystem.knowledge.enums.KnowledgeEntryType;
import com.example.dqcadirsystem.knowledge.enums.KnowledgeFileType;
import com.example.dqcadirsystem.knowledge.exception.KnowledgeErrorCode;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeFileRow;
import com.example.dqcadirsystem.knowledge.storage.StoredObject;
import com.example.dqcadirsystem.knowledge.validation.KnowledgeFileValidator;
import com.example.dqcadirsystem.knowledge.validation.ValidatedKnowledgeFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 批量上传请求级校验、顺序处理、部分成功和补偿策略测试。 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBatchUploadServiceTest {

    @Mock
    private KnowledgeFileValidator validator;
    @Mock
    private LongIdGenerator idGenerator;
    @Mock
    private KnowledgeFileStorageCoordinator storageCoordinator;
    @Mock
    private KnowledgeBatchUploadPersistenceService persistenceService;

    private KnowledgeBatchUploadService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeBatchUploadService(
                validator, idGenerator, storageCoordinator, persistenceService);
    }

    /** 同名文件可以独立成功，响应顺序、序号和汇总数量必须与请求一致。 */
    @Test
    void shouldUploadDuplicateNamesAsIndependentEntriesInRequestOrder() {
        MockMultipartFile first = pdf("same.pdf");
        MockMultipartFile second = pdf("same.pdf");
        ValidatedKnowledgeFile validated =
                new ValidatedKnowledgeFile("same.pdf", KnowledgeFileType.PDF, 8L);
        StoredObject firstStored = stored(11L, 12L);
        StoredObject secondStored = stored(21L, 22L);
        when(validator.validate(first)).thenReturn(validated);
        when(validator.validate(second)).thenReturn(validated);
        when(idGenerator.nextId()).thenReturn(11L, 12L, 21L, 22L);
        when(storageCoordinator.upload(11L, 12L, first, validated)).thenReturn(firstStored);
        when(storageCoordinator.upload(21L, 22L, second, validated)).thenReturn(secondStored);
        when(persistenceService.createPlaceholderEntryAndFile(
                11L, 12L, KnowledgeEntryType.DRAWING, "same", validated, firstStored))
                .thenReturn(row(11L, 12L, firstStored, LocalDateTime.of(2026, 7, 1, 10, 0)));
        when(persistenceService.createPlaceholderEntryAndFile(
                21L, 22L, KnowledgeEntryType.DRAWING, "same", validated, secondStored))
                .thenReturn(row(21L, 22L, secondStored, LocalDateTime.of(2026, 7, 1, 10, 1)));

        KnowledgeBatchUploadResponse response = service.upload(" DRAWING ", List.of(first, second));

        assertEquals(2, response.totalCount());
        assertEquals(2, response.successCount());
        assertEquals(0, response.failedCount());
        assertEquals(1, response.fileList().get(0).index());
        assertEquals("11", response.fileList().get(0).entryId());
        assertEquals(2, response.fileList().get(1).index());
        assertEquals("22", response.fileList().get(1).fileId());
        assertEquals("SUCCESS", response.fileList().get(1).uploadStatus());

        var order = inOrder(storageCoordinator, persistenceService);
        order.verify(storageCoordinator).upload(11L, 12L, first, validated);
        order.verify(persistenceService).createPlaceholderEntryAndFile(
                11L, 12L, KnowledgeEntryType.DRAWING, "same", validated, firstStored);
        order.verify(storageCoordinator).upload(21L, 22L, second, validated);
    }

    /** 单项格式校验失败应保留失败元数据，并继续处理后续合法文件。 */
    @Test
    void shouldContinueAfterValidationFailure() {
        MockMultipartFile invalid = new MockMultipartFile(
                "files", "bad.exe", "application/octet-stream", "bad".getBytes());
        MockMultipartFile valid = pdf("good.pdf");
        ValidatedKnowledgeFile validated =
                new ValidatedKnowledgeFile("good.pdf", KnowledgeFileType.PDF, 8L);
        StoredObject stored = stored(1L, 2L);
        when(validator.validate(invalid)).thenThrow(
                new BusinessException(CommonErrorCode.BAD_REQUEST, "仅支持PDF、Word、DWG和DXF文件"));
        when(validator.validate(valid)).thenReturn(validated);
        when(idGenerator.nextId()).thenReturn(1L, 2L);
        when(storageCoordinator.upload(1L, 2L, valid, validated)).thenReturn(stored);
        when(persistenceService.createPlaceholderEntryAndFile(
                1L, 2L, KnowledgeEntryType.LAW, "good", validated, stored))
                .thenReturn(row(1L, 2L, stored, LocalDateTime.now()));

        KnowledgeBatchUploadResponse response = service.upload("LAW", List.of(invalid, valid));

        assertEquals(1, response.successCount());
        assertEquals(1, response.failedCount());
        assertEquals("FAILED", response.fileList().get(0).uploadStatus());
        assertEquals("bad.exe", response.fileList().get(0).fileName());
        assertEquals("exe", response.fileList().get(0).fileType());
        assertNull(response.fileList().get(0).entryId());
        assertEquals("SUCCESS", response.fileList().get(1).uploadStatus());
    }

    /** 数据库失败时补偿本次 OSS 对象，并继续处理下一个文件。 */
    @Test
    void shouldCompensateDatabaseFailureAndContinue() {
        MockMultipartFile first = pdf("first.pdf");
        MockMultipartFile second = pdf("second.pdf");
        ValidatedKnowledgeFile firstValidated =
                new ValidatedKnowledgeFile("first.pdf", KnowledgeFileType.PDF, 8L);
        ValidatedKnowledgeFile secondValidated =
                new ValidatedKnowledgeFile("second.pdf", KnowledgeFileType.PDF, 8L);
        StoredObject firstStored = stored(1L, 2L);
        StoredObject secondStored = stored(3L, 4L);
        RuntimeException databaseFailure = new RuntimeException("SQL detail");
        when(validator.validate(first)).thenReturn(firstValidated);
        when(validator.validate(second)).thenReturn(secondValidated);
        when(idGenerator.nextId()).thenReturn(1L, 2L, 3L, 4L);
        when(storageCoordinator.upload(1L, 2L, first, firstValidated)).thenReturn(firstStored);
        when(storageCoordinator.upload(3L, 4L, second, secondValidated)).thenReturn(secondStored);
        when(persistenceService.createPlaceholderEntryAndFile(
                1L, 2L, KnowledgeEntryType.CASE, "first", firstValidated, firstStored))
                .thenThrow(databaseFailure);
        when(persistenceService.createPlaceholderEntryAndFile(
                3L, 4L, KnowledgeEntryType.CASE, "second", secondValidated, secondStored))
                .thenReturn(row(3L, 4L, secondStored, LocalDateTime.now()));

        KnowledgeBatchUploadResponse response = service.upload("CASE", List.of(first, second));

        assertEquals(1, response.successCount());
        assertEquals("文件处理失败，请稍后重试", response.fileList().get(0).errorMsg());
        assertEquals("SUCCESS", response.fileList().get(1).uploadStatus());
        verify(storageCoordinator).compensate(firstStored, databaseFailure);
    }

    /** OSS 依赖异常属于单项失败；即使全部文件失败，仍应返回完整明细而不是抛出请求异常。 */
    @Test
    void shouldReturnCompleteResultWhenAllStorageUploadsFail() {
        MockMultipartFile first = pdf("first.pdf");
        MockMultipartFile second = pdf("second.pdf");
        ValidatedKnowledgeFile firstValidated =
                new ValidatedKnowledgeFile("first.pdf", KnowledgeFileType.PDF, 8L);
        ValidatedKnowledgeFile secondValidated =
                new ValidatedKnowledgeFile("second.pdf", KnowledgeFileType.PDF, 8L);
        when(validator.validate(first)).thenReturn(firstValidated);
        when(validator.validate(second)).thenReturn(secondValidated);
        when(idGenerator.nextId()).thenReturn(1L, 2L, 3L, 4L);
        when(storageCoordinator.upload(1L, 2L, first, firstValidated))
                .thenThrow(new BusinessException(KnowledgeErrorCode.FILE_UPLOAD_FAILED));
        when(storageCoordinator.upload(3L, 4L, second, secondValidated))
                .thenThrow(new BusinessException(KnowledgeErrorCode.FILE_UPLOAD_FAILED));

        KnowledgeBatchUploadResponse response = service.upload("DRAWING", List.of(first, second));

        assertEquals(2, response.totalCount());
        assertEquals(0, response.successCount());
        assertEquals(2, response.failedCount());
        assertEquals("FAILED", response.fileList().get(0).uploadStatus());
        assertEquals("文件上传失败，请稍后重试", response.fileList().get(1).errorMsg());
        verify(persistenceService, never()).createPlaceholderEntryAndFile(
                anyLong(), anyLong(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectInvalidEntryTypeBeforeProcessingFiles() {
        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.upload("drawing", List.of(pdf("a.pdf"))));

        assertEquals("知识条目类型不正确", exception.getMessage());
        verify(validator, never()).validate(any());
    }

    @Test
    void shouldRejectEmptyAndTooManyFiles() {
        assertEquals("批量上传文件不能为空", assertThrows(
                BusinessException.class, () -> service.upload("DRAWING", List.of())).getMessage());

        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            files.add(pdf(i + ".pdf"));
        }
        assertEquals("单次批量上传不能超过10个文件", assertThrows(
                BusinessException.class, () -> service.upload("DRAWING", files)).getMessage());
        verify(storageCoordinator, never()).upload(any(), any(), any(), any());
    }

    /** 总大小校验使用声明大小，不需要在测试中实际分配 1 GiB 内存。 */
    @Test
    void shouldRejectTotalSizeOverOneGibBeforeProcessing() {
        MultipartFile first = sizedFile(600L * 1024 * 1024);
        MultipartFile second = sizedFile(500L * 1024 * 1024);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.upload("PROGRAM_RULE", List.of(first, second)));

        assertEquals("单次批量上传总大小不能超过1GiB", exception.getMessage());
        verify(validator, never()).validate(any());
    }

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile("files", name, "application/pdf", "%PDF-1.7".getBytes());
    }

    private MultipartFile sizedFile(long size) {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(size);
        return file;
    }

    private StoredObject stored(long entryId, long fileId) {
        String key = "knowledge/" + entryId + "/" + fileId + ".pdf";
        return new StoredObject(key, "https://bucket/" + key);
    }

    private KnowledgeFileRow row(
            long entryId, long fileId, StoredObject storedObject, LocalDateTime uploadedAt) {
        return new KnowledgeFileRow(
                Long.toString(fileId), Long.toString(entryId), "same.pdf", "pdf", 8L,
                storedObject.publicUrl(), "success", 1, uploadedAt);
    }
}
