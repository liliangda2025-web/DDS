package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.knowledge.enums.KnowledgeEntryType;
import com.example.dqcadirsystem.knowledge.mapper.KnowledgeEntryMapper;
import com.example.dqcadirsystem.knowledge.mapper.KnowledgeFileMapper;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeFileRow;
import com.example.dqcadirsystem.knowledge.storage.StoredObject;
import com.example.dqcadirsystem.knowledge.validation.ValidatedKnowledgeFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 批量导入中单个文件的独立数据库事务服务。
 *
 * <p>该类必须与外层批量编排服务分离为不同 Spring Bean，使 {@link Transactional} 代理生效。
 * 每个文件使用一个 {@code REQUIRES_NEW} 事务：当前文件失败只回滚自己的占位条目和文件记录，
 * 已成功提交的前序文件不会被后续失败连带回滚。</p>
 */
@Service
public class KnowledgeBatchUploadPersistenceService {

    private final KnowledgeEntryMapper knowledgeEntryMapper;
    private final KnowledgeFileMapper knowledgeFileMapper;

    public KnowledgeBatchUploadPersistenceService(
            KnowledgeEntryMapper knowledgeEntryMapper,
            KnowledgeFileMapper knowledgeFileMapper) {
        this.knowledgeEntryMapper = knowledgeEntryMapper;
        this.knowledgeFileMapper = knowledgeFileMapper;
    }

    /**
     * 原子写入待补充知识条目和当前文件，并读取数据库生成的上传时间。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public KnowledgeFileRow createPlaceholderEntryAndFile(
            long entryId,
            long fileId,
            KnowledgeEntryType entryType,
            String title,
            ValidatedKnowledgeFile file,
            StoredObject storedObject) {
        String entryCode = "TMP_" + entryId;
        int insertedEntries = knowledgeEntryMapper.insertBatchPlaceholderEntry(
                entryId, entryType.value(), entryCode, title);
        if (insertedEntries != 1) {
            throw new IllegalStateException("批量导入知识条目影响行数异常: " + insertedEntries);
        }

        int insertedFiles = knowledgeFileMapper.insertSuccessfulFile(
                fileId,
                entryId,
                file.originalFileName(),
                file.type().extension(),
                file.size(),
                storedObject.publicUrl());
        if (insertedFiles != 1) {
            throw new IllegalStateException("批量导入知识文件影响行数异常: " + insertedFiles);
        }

        KnowledgeFileRow row = knowledgeFileMapper.selectById(fileId);
        if (row == null) {
            throw new IllegalStateException("批量导入文件写入后无法读取: " + fileId);
        }
        return row;
    }
}
