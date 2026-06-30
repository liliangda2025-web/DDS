package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.common.id.LongIdGenerator;
import com.example.dqcadirsystem.knowledge.config.OssProperties;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeFileResponse;
import com.example.dqcadirsystem.knowledge.exception.KnowledgeErrorCode;
import com.example.dqcadirsystem.knowledge.mapper.KnowledgeFileMapper;
import com.example.dqcadirsystem.knowledge.storage.ObjectStorageException;
import com.example.dqcadirsystem.knowledge.storage.ObjectStorageService;
import com.example.dqcadirsystem.knowledge.storage.StoredObject;
import com.example.dqcadirsystem.knowledge.validation.KnowledgeFileValidator;
import com.example.dqcadirsystem.knowledge.validation.ValidatedKnowledgeFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 知识文件上传与替换的业务编排服务。
 *
 * <p>OSS 和关系数据库无法参与同一个本地事务，因此流程采用“先上传对象、再提交数据库、失败时删除
 * 新对象”的补偿策略。历史文件不会物理删除，以便后续实现历史版本查看。</p>
 */
@Service
public class KnowledgeFileService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeFileService.class);

    private final KnowledgeFileValidator fileValidator;
    private final KnowledgeFileMapper knowledgeFileMapper;
    private final LongIdGenerator longIdGenerator;
    private final ObjectStorageService objectStorageService;
    private final KnowledgeFilePersistenceService persistenceService;
    private final OssProperties ossProperties;

    public KnowledgeFileService(
            KnowledgeFileValidator fileValidator,
            KnowledgeFileMapper knowledgeFileMapper,
            LongIdGenerator longIdGenerator,
            ObjectStorageService objectStorageService,
            KnowledgeFilePersistenceService persistenceService,
            OssProperties ossProperties) {
        this.fileValidator = fileValidator;
        this.knowledgeFileMapper = knowledgeFileMapper;
        this.longIdGenerator = longIdGenerator;
        this.objectStorageService = objectStorageService;
        this.persistenceService = persistenceService;
        this.ossProperties = ossProperties;
    }

    /**
     * 上传新文件并把它设置为指定知识条目的当前文件。
     */
    public KnowledgeFileResponse uploadOrReplaceCurrentFile(Long entryId, MultipartFile multipartFile) {
        ValidatedKnowledgeFile file = fileValidator.validate(multipartFile);
        if (knowledgeFileMapper.selectActiveEntryId(entryId) == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "知识条目不存在");
        }

        long fileId = longIdGenerator.nextId();
        String objectKey = buildObjectKey(entryId, fileId, file.type().extension());
        StoredObject storedObject = uploadToStorage(multipartFile, file, objectKey);

        try {
            return persistenceService.replaceCurrentFile(entryId, fileId, file, storedObject);
        } catch (RuntimeException databaseException) {
            compensateUploadedObject(storedObject.objectKey(), databaseException);
            throw databaseException;
        }
    }

    /** 上传过程不处于数据库事务中，防止慢网络长期占用数据库连接。 */
    private StoredObject uploadToStorage(
            MultipartFile multipartFile, ValidatedKnowledgeFile file, String objectKey) {
        try (InputStream inputStream = multipartFile.getInputStream()) {
            return objectStorageService.upload(
                    objectKey, inputStream, file.size(), file.type().contentType());
        } catch (IOException | ObjectStorageException exception) {
            log.error("Knowledge file upload failed: entryId={}, objectKey={}",
                    objectKeyEntryId(objectKey), objectKey, exception);
            throw new BusinessException(KnowledgeErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 数据库失败后尽力删除刚上传的孤儿对象。补偿失败不会掩盖原始数据库异常，
     * 而是作为 suppressed exception 附加，日志中可以同时看到两条故障链。
     */
    private void compensateUploadedObject(String objectKey, RuntimeException originalException) {
        try {
            objectStorageService.delete(objectKey);
        } catch (ObjectStorageException compensationException) {
            originalException.addSuppressed(compensationException);
            log.error("Failed to compensate uploaded OSS object: objectKey={}",
                    objectKey, compensationException);
        }
    }

    /** 生成只包含数字、固定目录和受控扩展名的安全唯一 Key。 */
    private String buildObjectKey(Long entryId, Long fileId, String extension) {
        String prefix = ossProperties.keyPrefix();
        prefix = prefix == null ? "knowledge" : prefix.replaceAll("^/+|/+$", "");
        return prefix + "/" + entryId + "/" + fileId + "." + extension;
    }

    /** 日志辅助方法，避免额外保存已知的 entryId 状态；解析失败时返回完整 Key 仍可排查。 */
    private String objectKeyEntryId(String objectKey) {
        String[] segments = objectKey.split("/");
        return segments.length >= 2 ? segments[segments.length - 2] : objectKey;
    }
}
