package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeFileResponse;
import com.example.dqcadirsystem.knowledge.mapper.KnowledgeFileMapper;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeFileRow;
import com.example.dqcadirsystem.knowledge.storage.StoredObject;
import com.example.dqcadirsystem.knowledge.validation.ValidatedKnowledgeFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 当前文件数据库替换事务。
 *
 * <p>该类与 OSS 编排服务拆成两个 Spring Bean，是为了让 {@link Transactional} 代理真正生效，
 * 同时确保耗时的网络上传不占用数据库连接和行锁。</p>
 */
@Service
public class KnowledgeFilePersistenceService {

    private final KnowledgeFileMapper knowledgeFileMapper;

    public KnowledgeFilePersistenceService(KnowledgeFileMapper knowledgeFileMapper) {
        this.knowledgeFileMapper = knowledgeFileMapper;
    }

    /**
     * 锁定条目、降级历史当前文件、插入新文件，并读取数据库生成的上传时间。
     */
    @Transactional
    public KnowledgeFileResponse replaceCurrentFile(
            Long entryId,
            Long fileId,
            ValidatedKnowledgeFile file,
            StoredObject storedObject) {
        if (knowledgeFileMapper.selectActiveEntryIdForUpdate(entryId) == null) {
            throw entryNotFoundException();
        }

        knowledgeFileMapper.unsetCurrentFiles(entryId);
        int insertedRows = knowledgeFileMapper.insertSuccessfulFile(
                fileId,
                entryId,
                file.originalFileName(),
                file.type().extension(),
                file.size(),
                storedObject.publicUrl());
        if (insertedRows != 1) {
            throw new IllegalStateException("新增知识文件影响行数异常: " + insertedRows);
        }

        KnowledgeFileRow row = knowledgeFileMapper.selectById(fileId);
        if (row == null) {
            throw new IllegalStateException("知识文件写入后无法读取: " + fileId);
        }
        return new KnowledgeFileResponse(
                row.fileId(), row.entryId(), row.originalFileName(), row.fileType(), row.fileSize(),
                row.fileUrl(), row.uploadStatus(), row.isCurrent(), row.uploadedAt());
    }

    private BusinessException entryNotFoundException() {
        return new BusinessException(CommonErrorCode.NOT_FOUND, "知识条目不存在");
    }
}
