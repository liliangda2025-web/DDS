package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryUpdateRequest;
import com.example.dqcadirsystem.knowledge.mapper.KnowledgeEntryMapper;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeEntryInfoImportFileRow;
import com.example.dqcadirsystem.knowledge.service.model.KnowledgeEntryInfoImportRow;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;

/**
 * 批量信息导入的单行事务服务。
 *
 * <p>必须与批量编排服务拆成独立 Spring Bean，{@code REQUIRES_NEW} 才能经过代理生效。每行拥有独立事务，
 * 一行校验或写入失败只回滚当前行，已经成功的其他行不受影响。</p>
 */
@Service
public class KnowledgeEntryInfoImportTransactionService {

    private static final String BUSINESS_KEY_CONSTRAINT = "uk_entry_type_code_version_marker";

    private final KnowledgeEntryMapper knowledgeEntryMapper;

    public KnowledgeEntryInfoImportTransactionService(KnowledgeEntryMapper knowledgeEntryMapper) {
        this.knowledgeEntryMapper = knowledgeEntryMapper;
    }

    /** 锁定目标、核对系统列和唯一键，并原子完成一行回填。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void importRow(KnowledgeEntryInfoImportRow row) {
        Integer infoStatus = knowledgeEntryMapper.selectActiveInfoStatusForUpdate(row.entryId());
        if (infoStatus == null) {
            throw rowError("知识条目不存在或已删除");
        }
        if (infoStatus != 0) {
            throw rowError("知识条目已完善");
        }

        KnowledgeEntryInfoImportFileRow file =
                knowledgeEntryMapper.selectValidImportFile(row.entryId(), row.fileId());
        if (file == null) {
            throw rowError("文件不存在、已失效或不是当前成功文件");
        }
        validateSystemColumns(row, file);

        KnowledgeEntryUpdateRequest updateRequest = row.toUpdateRequest();
        if (knowledgeEntryMapper.countActiveByBusinessKey(updateRequest, row.entryId()) > 0) {
            throw duplicateBusinessKey();
        }

        try {
            int updatedRows = knowledgeEntryMapper.updateEntry(row.entryId(), updateRequest);
            if (updatedRows != 1) {
                throw new IllegalStateException("批量回填知识条目影响行数异常: " + updatedRows);
            }
        } catch (DuplicateKeyException exception) {
            if (isBusinessKeyViolation(exception)) {
                throw duplicateBusinessKey();
            }
            throw exception;
        }
    }

    /**
     * Excel 保护仅用于减少误操作，真正的可信边界是数据库。所有系统识别字段必须和数据库一致，
     * 上传时间按秒比较，以兼容 Excel 和数据库对亚秒精度的不同保存能力。
     */
    private void validateSystemColumns(
            KnowledgeEntryInfoImportRow row, KnowledgeEntryInfoImportFileRow file) {
        boolean uploadedAtMatches = row.uploadedAt() != null && file.uploadedAt() != null
                && row.uploadedAt().truncatedTo(ChronoUnit.SECONDS)
                .equals(file.uploadedAt().truncatedTo(ChronoUnit.SECONDS));
        if (!Objects.equals(row.fileName(), file.originalFileName())
                || !Objects.equals(row.fileType(), file.fileType())
                || !Objects.equals(row.fileSize(), file.fileSize())
                || !uploadedAtMatches
                || !Objects.equals(row.uploadStatus(), file.uploadStatus())) {
            throw rowError("系统识别字段已被修改，请重新导出模板");
        }
    }

    private KnowledgeEntryInfoImportRowException duplicateBusinessKey() {
        return rowError("相同类型、条目编码和版本的知识条目已存在");
    }

    private KnowledgeEntryInfoImportRowException rowError(String message) {
        return new KnowledgeEntryInfoImportRowException(message);
    }

    /** 只把明确命中业务唯一键的重复异常转换成可展示的单行失败。 */
    private boolean isBusinessKeyViolation(DuplicateKeyException exception) {
        Throwable cause = exception;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null
                    && message.toLowerCase(Locale.ROOT).contains(BUSINESS_KEY_CONSTRAINT)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
