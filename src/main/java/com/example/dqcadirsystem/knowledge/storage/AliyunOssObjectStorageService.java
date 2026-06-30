package com.example.dqcadirsystem.knowledge.storage;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.models.DeleteObjectRequest;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import com.example.dqcadirsystem.knowledge.config.OssProperties;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * 基于阿里云 OSS Java SDK V2 的对象存储实现。
 */
@Service
public class AliyunOssObjectStorageService implements ObjectStorageService {

    private final OSSClient ossClient;
    private final OssProperties properties;

    public AliyunOssObjectStorageService(OSSClient ossClient, OssProperties properties) {
        this.ossClient = ossClient;
        this.properties = properties;
    }

    /**
     * 使用已知长度的输入流上传文件，并明确禁止覆盖同名对象。
     *
     * <p>文件 ID 已经使 Key 唯一，{@code forbidOverwrite=true} 是第二道保护：如果雪花节点配置错误
     * 导致 Key 碰撞，OSS 会拒绝写入，而不会静默覆盖一份已有文件。</p>
     */
    @Override
    public StoredObject upload(
            String objectKey, InputStream inputStream, long contentLength, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.newBuilder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .forbidOverwrite(true)
                    .contentLength(Math.toIntExact(contentLength))
                    .contentType(contentType)
                    .body(BinaryData.fromStream(inputStream, contentLength))
                    .build();
            ossClient.putObject(request);
            return new StoredObject(objectKey, buildPublicUrl(objectKey));
        } catch (Exception exception) {
            throw new ObjectStorageException("上传文件到 OSS 失败", exception);
        }
    }

    /**
     * 删除补偿对象。OSS 的删除操作本身具有幂等性，对象已经不存在时也可视为补偿完成。
     */
    @Override
    public void delete(String objectKey) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.newBuilder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .build();
            ossClient.deleteObject(request);
        } catch (Exception exception) {
            throw new ObjectStorageException("删除 OSS 补偿对象失败", exception);
        }
    }

    /** 去掉配置中可能存在的尾部斜杠，保证最终 URL 中只有一个路径分隔符。 */
    private String buildPublicUrl(String objectKey) {
        String baseUrl = properties.publicBaseUrl();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/" + objectKey;
    }
}
