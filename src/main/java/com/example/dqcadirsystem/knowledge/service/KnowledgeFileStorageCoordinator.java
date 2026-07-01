package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.knowledge.config.OssProperties;
import com.example.dqcadirsystem.knowledge.exception.KnowledgeErrorCode;
import com.example.dqcadirsystem.knowledge.storage.ObjectStorageException;
import com.example.dqcadirsystem.knowledge.storage.ObjectStorageService;
import com.example.dqcadirsystem.knowledge.storage.StoredObject;
import com.example.dqcadirsystem.knowledge.validation.ValidatedKnowledgeFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 知识文件 OSS 上传与失败补偿的共享协调器。
 *
 * <p>单文件替换和批量导入必须使用完全相同的对象 Key、Content-Type 和安全异常策略。
 * 把这些逻辑集中在独立组件中，可以避免后续修改存储规则时两类接口出现行为差异。</p>
 */
@Service
public class KnowledgeFileStorageCoordinator {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeFileStorageCoordinator.class);

    private final ObjectStorageService objectStorageService;
    private final OssProperties ossProperties;

    public KnowledgeFileStorageCoordinator(
            ObjectStorageService objectStorageService,
            OssProperties ossProperties) {
        this.objectStorageService = objectStorageService;
        this.ossProperties = ossProperties;
    }

    /**
     * 在数据库事务之外把文件流上传至 OSS。
     *
     * <p>输入流只在本方法调用期间打开，OSS 适配层完成同步读取后立即关闭。底层 SDK 或文件读取
     * 细节只写入服务端日志，对调用方统一返回可安全展示的 50200 提示。</p>
     */
    public StoredObject upload(
            Long entryId,
            Long fileId,
            MultipartFile multipartFile,
            ValidatedKnowledgeFile file) {
        String objectKey = buildObjectKey(entryId, fileId, file.type().extension());
        try (InputStream inputStream = multipartFile.getInputStream()) {
            return objectStorageService.upload(
                    objectKey, inputStream, file.size(), file.type().contentType());
        } catch (IOException | ObjectStorageException exception) {
            log.error("Knowledge file upload failed: entryId={}, fileId={}, objectKey={}",
                    entryId, fileId, objectKey, exception);
            throw new BusinessException(KnowledgeErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 数据库提交失败后尽力删除刚上传的 OSS 对象。
     *
     * <p>补偿删除失败不会覆盖最初的数据库异常，而是作为 suppressed exception 附加并记录。
     * 这样业务层仍按原始失败处理，运维日志又能保留两段故障链。</p>
     */
    public void compensate(StoredObject storedObject, RuntimeException originalException) {
        try {
            objectStorageService.delete(storedObject.objectKey());
        } catch (ObjectStorageException compensationException) {
            originalException.addSuppressed(compensationException);
            log.error("Failed to compensate uploaded OSS object: objectKey={}",
                    storedObject.objectKey(), compensationException);
        }
    }

    /** 生成只包含受控目录、数字 ID 和标准扩展名的唯一对象 Key。 */
    private String buildObjectKey(Long entryId, Long fileId, String extension) {
        String prefix = ossProperties.keyPrefix();
        prefix = prefix == null ? "knowledge" : prefix.replaceAll("^/+|/+$", "");
        return prefix + "/" + entryId + "/" + fileId + "." + extension;
    }
}
