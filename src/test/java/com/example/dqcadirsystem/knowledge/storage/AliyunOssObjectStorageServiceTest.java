package com.example.dqcadirsystem.knowledge.storage;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.models.DeleteObjectRequest;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.example.dqcadirsystem.knowledge.config.OssProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

/** 验证 OSS 适配器生成的 SDK 请求，不连接真实阿里云服务。 */
@ExtendWith(MockitoExtension.class)
class AliyunOssObjectStorageServiceTest {

    @Mock
    private OSSClient ossClient;

    @Test
    void shouldBuildPutObjectRequest() {
        OssProperties properties = new OssProperties(
                "cn-beijing", "https://oss-cn-beijing.aliyuncs.com", "liliangda-oss-test",
                "https://liliangda-oss-test.oss-cn-beijing.aliyuncs.com/", "knowledge");
        AliyunOssObjectStorageService service = new AliyunOssObjectStorageService(ossClient, properties);
        String key = "knowledge/1/2.pdf";

        StoredObject result = service.upload(
                key, new ByteArrayInputStream("%PDF-".getBytes()), 5L, "application/pdf");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(ossClient).putObject(captor.capture());
        PutObjectRequest request = captor.getValue();
        assertEquals("liliangda-oss-test", request.bucket());
        assertEquals(key, request.key());
        assertEquals("application/pdf", request.contentType());
        assertEquals(5, request.contentLength());
        assertTrue(request.forbidOverwrite());
        assertEquals("https://liliangda-oss-test.oss-cn-beijing.aliyuncs.com/" + key,
                result.publicUrl());
    }

    @Test
    void shouldBuildDeleteObjectRequest() {
        OssProperties properties = new OssProperties(
                "cn-beijing", "endpoint", "bucket", "https://bucket", "knowledge");
        AliyunOssObjectStorageService service = new AliyunOssObjectStorageService(ossClient, properties);

        service.delete("knowledge/1/2.pdf");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(ossClient).deleteObject(captor.capture());
        assertEquals("bucket", captor.getValue().bucket());
        assertEquals("knowledge/1/2.pdf", captor.getValue().key());
    }
}
