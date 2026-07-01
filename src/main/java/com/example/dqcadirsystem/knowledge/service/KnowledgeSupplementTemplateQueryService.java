package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeSupplementTemplateExportRequest;
import com.example.dqcadirsystem.knowledge.mapper.KnowledgeEntryMapper;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeSupplementTemplateRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识条目信息补充模板的只读数据库查询服务。
 *
 * <p>查询与 Excel 生成拆成不同 Bean，是为了让只读事务只覆盖数据库访问。查询结果返回后连接即可
 * 释放，后续内存中的工作簿构建不会长期占用数据库事务和连接。</p>
 */
@Service
public class KnowledgeSupplementTemplateQueryService {

    private final KnowledgeEntryMapper knowledgeEntryMapper;

    public KnowledgeSupplementTemplateQueryService(KnowledgeEntryMapper knowledgeEntryMapper) {
        this.knowledgeEntryMapper = knowledgeEntryMapper;
    }

    /** 精确查询请求中的全部待补充条目；返回顺序由外层服务按请求顺序重新整理。 */
    @Transactional(readOnly = true)
    public List<KnowledgeSupplementTemplateRow> queryExact(List<Long> entryIds) {
        return knowledgeEntryMapper.selectPendingSupplementByEntryIds(entryIds);
    }

    /** 条件查询时额外读取第 5001 条，用于判断是否超过单次导出上限。 */
    @Transactional(readOnly = true)
    public List<KnowledgeSupplementTemplateRow> queryByFilter(
            KnowledgeSupplementTemplateExportRequest request,
            int limit) {
        return knowledgeEntryMapper.selectPendingSupplementByFilter(request, limit);
    }
}
