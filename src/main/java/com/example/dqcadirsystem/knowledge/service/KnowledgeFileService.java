package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.common.id.LongIdGenerator;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeFileResponse;
import com.example.dqcadirsystem.knowledge.mapper.KnowledgeFileMapper;
import com.example.dqcadirsystem.knowledge.storage.StoredObject;
import com.example.dqcadirsystem.knowledge.validation.KnowledgeFileValidator;
import com.example.dqcadirsystem.knowledge.validation.ValidatedKnowledgeFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识文件上传与替换的业务编排服务。
 *
 * <p>OSS 和关系数据库无法参与同一个本地事务，因此流程采用“先上传对象、再提交数据库、失败时删除
 * 新对象”的补偿策略。历史文件不会物理删除，以便后续实现历史版本查看。</p>
 */
@Service
public class KnowledgeFileService {

    private final KnowledgeFileValidator fileValidator;
    private final KnowledgeFileMapper knowledgeFileMapper;
    private final LongIdGenerator longIdGenerator;
    private final KnowledgeFileStorageCoordinator storageCoordinator;
    private final KnowledgeFilePersistenceService persistenceService;

    public KnowledgeFileService(
            KnowledgeFileValidator fileValidator,
            KnowledgeFileMapper knowledgeFileMapper,
            LongIdGenerator longIdGenerator,
            KnowledgeFileStorageCoordinator storageCoordinator,
            KnowledgeFilePersistenceService persistenceService) {
        this.fileValidator = fileValidator;
        this.knowledgeFileMapper = knowledgeFileMapper;
        this.longIdGenerator = longIdGenerator;
        this.storageCoordinator = storageCoordinator;
        this.persistenceService = persistenceService;
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
        StoredObject storedObject = storageCoordinator.upload(entryId, fileId, multipartFile, file);

        try {
            return persistenceService.replaceCurrentFile(entryId, fileId, file, storedObject);
        } catch (RuntimeException databaseException) {
            storageCoordinator.compensate(storedObject, databaseException);
            throw databaseException;
        }
    }
}
