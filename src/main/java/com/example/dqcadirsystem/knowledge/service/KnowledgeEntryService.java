package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.api.PageResponse;
import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryPageRequest;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryDetailResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryPageItemResponse;
import com.example.dqcadirsystem.knowledge.enums.KnowledgeEntryType;
import com.example.dqcadirsystem.knowledge.mapper.KnowledgeEntryMapper;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeEntryDetailRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识条目业务服务。
 */
@Service
public class KnowledgeEntryService {

    private final KnowledgeEntryMapper knowledgeEntryMapper;

    public KnowledgeEntryService(KnowledgeEntryMapper knowledgeEntryMapper) {
        this.knowledgeEntryMapper = knowledgeEntryMapper;
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

    private void validateRequest(KnowledgeEntryPageRequest request) {
        if (request.entryType() != null && !KnowledgeEntryType.isValid(request.entryType())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "知识条目类型不合法");
        }
        if (request.startDate() != null && request.endDate() != null
                && request.startDate().isAfter(request.endDate())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "发版开始日期不能晚于结束日期");
        }
    }
}
