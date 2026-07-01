package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeSearchRequest;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeSearchItemResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeSearchResponse;
import com.example.dqcadirsystem.knowledge.enums.KnowledgeEntryType;
import com.example.dqcadirsystem.knowledge.mapper.KnowledgeEntryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 当前版本的知识检索服务。
 *
 * <p>检索使用数据库多字段 LIKE 匹配，不伪造语义相关度或匹配分数。计数和分页查询位于同一只读事务，
 * 响应耗时覆盖参数业务校验之后的数据库查询和结果转换过程。</p>
 */
@Service
public class KnowledgeSearchService {

    private final KnowledgeEntryMapper knowledgeEntryMapper;

    public KnowledgeSearchService(KnowledgeEntryMapper knowledgeEntryMapper) {
        this.knowledgeEntryMapper = knowledgeEntryMapper;
    }

    /** 执行检索并返回页面需要的统计信息、友好提示和卡片数据。 */
    @Transactional(readOnly = true)
    public KnowledgeSearchResponse search(KnowledgeSearchRequest request) {
        validateEntryType(request.entryType());
        long startNanos = System.nanoTime();

        long total = knowledgeEntryMapper.countSearch(request);
        List<KnowledgeSearchItemResponse> records = total == 0
                ? List.of()
                : knowledgeEntryMapper.selectSearch(request).stream()
                .map(KnowledgeSearchItemResponse::from)
                .toList();

        BigDecimal costSeconds = elapsedSeconds(startNanos);
        long pages = total == 0 ? 0 : (total + request.pageSize() - 1) / request.pageSize();
        String messageText = total == 0
                ? "查无结果，请调整关键词后重试"
                : buildSuccessMessage(total, request.entryType(), costSeconds);
        return new KnowledgeSearchResponse(
                total, request.pageNum(), request.pageSize(), pages, costSeconds, messageText, records);
    }

    private void validateEntryType(String entryType) {
        if (entryType != null && !KnowledgeEntryType.isValid(entryType)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "知识条目类型不合法");
        }
    }

    private BigDecimal elapsedSeconds(long startNanos) {
        long elapsedNanos = Math.max(0, System.nanoTime() - startNanos);
        return BigDecimal.valueOf(elapsedNanos)
                .divide(BigDecimal.valueOf(1_000_000_000L), 2, RoundingMode.HALF_UP);
    }

    private String buildSuccessMessage(long total, String entryType, BigDecimal costSeconds) {
        String resultName = KnowledgeEntryType.DRAWING.value().equals(entryType) ? "图纸" : "资料";
        return "共找到" + total + "份相关" + resultName + "，耗时" + costSeconds.toPlainString() + "s";
    }
}
