package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.api.PageResponse;
import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.common.id.LongIdGenerator;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryCreateRequest;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryPageRequest;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryUpdateRequest;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryCreateResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryDetailResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryPageItemResponse;
import com.example.dqcadirsystem.knowledge.enums.KnowledgeEntryType;
import com.example.dqcadirsystem.knowledge.mapper.KnowledgeEntryMapper;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeEntryDetailRow;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识条目业务服务。
 */
@Service
public class KnowledgeEntryService {

    private final KnowledgeEntryMapper knowledgeEntryMapper;
    private final LongIdGenerator longIdGenerator;

    public KnowledgeEntryService(KnowledgeEntryMapper knowledgeEntryMapper, LongIdGenerator longIdGenerator) {
        this.knowledgeEntryMapper = knowledgeEntryMapper;
        this.longIdGenerator = longIdGenerator;
    }

    /**
     * 按管理页面条件分页查询知识条目。
     *
     * <p>计数和列表查询放在同一个只读事务中，使两次查询尽可能观察到一致的数据快照。总数为 0 时
     * 直接返回空列表，避免执行没有意义的分页 SQL。</p>
     */
    @Transactional(readOnly = true)
    public PageResponse<KnowledgeEntryPageItemResponse> pageEntries(KnowledgeEntryPageRequest request) {
        validateRequest(request);

        long total = knowledgeEntryMapper.countPage(request);
        if (total == 0) {
            return PageResponse.of(0, request.pageNum(), request.pageSize(), List.of());
        }

        List<KnowledgeEntryPageItemResponse> records = knowledgeEntryMapper.selectPage(request).stream()
                .map(KnowledgeEntryPageItemResponse::from)
                .toList();
        return PageResponse.of(total, request.pageNum(), request.pageSize(), records);
    }

    /**
     * 查询单个知识条目的完整业务信息和当前有效文件。
     *
     * <p>Mapper 只查询正常状态的数据，因此“ID 不存在”和“条目已逻辑删除”对接口调用方表现一致，
     * 均返回资源不存在，避免泄露已经删除的数据。合法条目没有当前文件时仍可以返回详情，
     * 响应中的 {@code currentFile} 为 {@code null}。</p>
     */
    @Transactional(readOnly = true)
    public KnowledgeEntryDetailResponse getEntryDetail(Long entryId) {
        KnowledgeEntryDetailRow row = knowledgeEntryMapper.selectDetail(entryId);
        if (row == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "知识条目不存在");
        }
        return KnowledgeEntryDetailResponse.from(row);
    }

    /**
     * 手工新增一条信息完整的知识条目。
     *
     * <p>先执行可读性更好的业务重复检查，再依赖数据库唯一键处理并发竞争：两个相同请求即使同时通过前置检查，
     * 最终也只能有一个插入成功。手工新增只创建条目元数据，文件由独立上传接口后续关联。</p>
     */
    @Transactional
    public KnowledgeEntryCreateResponse createEntry(KnowledgeEntryCreateRequest request) {
        validateEntryType(request.entryType());
        if (knowledgeEntryMapper.countActiveByBusinessKey(request, null) > 0) {
            throw duplicateEntryException();
        }

        long entryId = longIdGenerator.nextId();
        try {
            int insertedRows = knowledgeEntryMapper.insertEntry(entryId, request);
            if (insertedRows != 1) {
                // 插入行数异常属于系统实现或数据库故障，不应伪装成可预期的业务错误。
                throw new IllegalStateException("新增知识条目影响行数异常: " + insertedRows);
            }
        } catch (DuplicateKeyException exception) {
            // 只有业务键确实已经存在时才转换提示；若是节点号配置错误造成主键冲突，则保留系统异常以便排查。
            if (knowledgeEntryMapper.countActiveByBusinessKey(request, null) > 0) {
                throw duplicateEntryException();
            }
            throw exception;
        }
        return KnowledgeEntryCreateResponse.from(entryId);
    }

    /**
     * 修改正常状态知识条目的业务元数据。
     *
     * <p>重复检查排除当前条目自身，因此原值保存属于合法的幂等操作。更新 SQL 只操作 {@code status = 1}
     * 的记录，并在影响行数为 0 时再次确认条目状态，以兼容数据库“值没有变化时返回 0”的配置。</p>
     */
    @Transactional
    public boolean updateEntry(Long entryId, KnowledgeEntryUpdateRequest request) {
        validateEntryType(request.entryType());
        ensureEntryExists(entryId);

        if (knowledgeEntryMapper.countActiveByBusinessKey(request, entryId) > 0) {
            throw duplicateEntryException();
        }

        try {
            int updatedRows = knowledgeEntryMapper.updateEntry(entryId, request);
            if (updatedRows > 1) {
                throw new IllegalStateException("修改知识条目影响行数异常: " + updatedRows);
            }
            if (updatedRows == 0 && knowledgeEntryMapper.countActiveById(entryId) == 0) {
                // 前置检查后条目可能被另一事务逻辑删除，此时仍应按资源不存在响应。
                throw entryNotFoundException();
            }
        } catch (DuplicateKeyException exception) {
            // 并发修改可能在前置检查后占用目标业务键，数据库唯一键负责最终防线。
            if (knowledgeEntryMapper.countActiveByBusinessKey(request, entryId) > 0) {
                throw duplicateEntryException();
            }
            throw exception;
        }
        return true;
    }

    private void validateRequest(KnowledgeEntryPageRequest request) {
        if (request.entryType() != null) {
            validateEntryType(request.entryType());
        }
        if (request.startDate() != null && request.endDate() != null
                && request.startDate().isAfter(request.endDate())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "发版开始日期不能晚于结束日期");
        }
    }

    /** 多个知识条目接口共用同一套类型合法性判断和错误提示。 */
    private void validateEntryType(String entryType) {
        if (!KnowledgeEntryType.isValid(entryType)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "知识条目类型不合法");
        }
    }

    /** 正常条目不存在时抛出与详情接口一致的 404 业务异常。 */
    private void ensureEntryExists(Long entryId) {
        if (knowledgeEntryMapper.countActiveById(entryId) == 0) {
            throw entryNotFoundException();
        }
    }

    private BusinessException entryNotFoundException() {
        return new BusinessException(CommonErrorCode.NOT_FOUND, "知识条目不存在");
    }

    /** 构造业务键重复异常，保证前置检查和数据库兜底使用完全一致的响应。 */
    private BusinessException duplicateEntryException() {
        return new BusinessException(
                CommonErrorCode.BUSINESS_ERROR,
                "相同类型、条目编码和版本的知识条目已存在");
    }
}
