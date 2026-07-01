package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.knowledge.config.OssProperties;
import com.example.dqcadirsystem.knowledge.enums.KnowledgeFileType;
import com.example.dqcadirsystem.knowledge.exception.KnowledgeErrorCode;
import com.example.dqcadirsystem.knowledge.storage.ObjectStorageException;
import com.example.dqcadirsystem.knowledge.storage.ObjectStorageService;
import com.example.dqcadirsystem.knowledge.storage.StoredObject;
import com.example.dqcadirsystem.knowledge.validation.ValidatedKnowledgeFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 单文件与批量上传共享的 OSS Key、异常转换和补偿测试。 */
@ExtendWith(MockitoExtension.class)
class KnowledgeFileStorageCoordinatorTest {

    @Mock
    private ObjectStorageService storageService;

    private KnowledgeFileStorageCoordinator coordinator;
    private MockMultipartFile multipartFile;
    private ValidatedKnowledgeFile validatedFile;

    @BeforeEach
    void setUp() {
        coordinator = new KnowledgeFileStorageCoordinator(
                storageService,
                new OssProperties("cn-beijing", "endpoint", "bucket", "base", "/knowledge/"));
        multipartFile = new MockMultipartFile(
                "file", "drawing.pdf", "application/pdf", "%PDF-1.7".getBytes());
        validatedFile = new ValidatedKnowledgeFile("drawing.pdf", KnowledgeFileType.PDF, 8L);
    }

    @Test
    void shouldBuildStableKeyAndUploadWithValidatedMetadata() {
        String key = "knowledge/1/2.pdf";
        StoredObject expected = new StoredObject(key, "https://bucket/" + key);
        when(storageService.upload(eq(key), any(InputStream.class), eq(8L), eq("application/pdf")))
                .thenReturn(expected);

        assertSame(expected, coordinator.upload(1L, 2L, multipartFile, validatedFile));
    }

    @Test
    void shouldTranslateStorageFailureToSafeBusinessError() {
        when(storageService.upload(any(), any(), eq(8L), any()))
                .thenThrow(new ObjectStorageException("SDK detail", new RuntimeException()));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> coordinator.upload(1L, 2L, multipartFile, validatedFile));

        assertEquals(KnowledgeErrorCode.FILE_UPLOAD_FAILED, exception.getErrorCode());
    }

    @Test
    void shouldAttachCompensationFailureWithoutReplacingOriginalError() {
        StoredObject stored = new StoredObject("knowledge/1/2.pdf", "https://bucket/key");
        RuntimeException original = new RuntimeException("database failed");
        ObjectStorageException cleanup = new ObjectStorageException("cleanup failed", null);
        doThrow(cleanup).when(storageService).delete(stored.objectKey());

        coordinator.compensate(stored, original);

        assertSame(cleanup, original.getSuppressed()[0]);
        verify(storageService).delete(stored.objectKey());
    }
}
