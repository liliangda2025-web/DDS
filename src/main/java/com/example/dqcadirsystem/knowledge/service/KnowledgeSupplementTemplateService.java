package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeSupplementTemplateExportRequest;
import com.example.dqcadirsystem.knowledge.enums.KnowledgeEntryType;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeSupplementTemplateRow;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 知识条目信息补充模板导出的业务编排服务。
 *
 * <p>导出是幂等只读操作：查询和生成 Excel 都不会更新 {@code info_status} 或保存“已导出”标记。
 * 因此导出失败或用户丢失文件后可以直接重试；只有后续信息导入成功才会让记录退出待补充集合。</p>
 */
@Service
public class KnowledgeSupplementTemplateService {

    public static final int MAX_EXPORT_ROWS = 5000;
    private static final int FILTER_QUERY_LIMIT = MAX_EXPORT_ROWS + 1;
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final KnowledgeSupplementTemplateQueryService queryService;
    private final KnowledgeSupplementTemplateExcelWriter excelWriter;

    public KnowledgeSupplementTemplateService(
            KnowledgeSupplementTemplateQueryService queryService,
            KnowledgeSupplementTemplateExcelWriter excelWriter) {
        this.queryService = queryService;
        this.excelWriter = excelWriter;
    }

    /** 校验请求、查询完整数据集、一次性生成工作簿并返回文件。 */
    public KnowledgeSupplementTemplateExportFile export(KnowledgeSupplementTemplateExportRequest request) {
        validateRequest(request);
        List<KnowledgeSupplementTemplateRow> rows = request.exactMode()
                ? queryExactInRequestOrder(request.entryIds())
                : queryByFilter(request);

        if (rows.isEmpty()) {
            throw badRequest("没有可导出的待补充记录");
        }
        byte[] content = excelWriter.write(rows);
        String filename = "knowledge_supplement_template_"
                + LocalDateTime.now().format(FILE_TIME_FORMAT) + ".xlsx";
        return new KnowledgeSupplementTemplateExportFile(filename, content);
    }

    private List<KnowledgeSupplementTemplateRow> queryExactInRequestOrder(List<Long> entryIds) {
        List<KnowledgeSupplementTemplateRow> rows = queryService.queryExact(entryIds);
        if (rows.size() != entryIds.size()) {
            throw badRequest("部分知识条目不存在、已完善或没有有效文件");
        }

        Map<String, KnowledgeSupplementTemplateRow> rowByEntryId = new HashMap<>();
        for (KnowledgeSupplementTemplateRow row : rows) {
            if (rowByEntryId.put(row.entryId(), row) != null) {
                throw new IllegalStateException("补充模板查询返回重复知识条目: " + row.entryId());
            }
        }
        List<KnowledgeSupplementTemplateRow> orderedRows = new java.util.ArrayList<>(entryIds.size());
        for (Long entryId : entryIds) {
            KnowledgeSupplementTemplateRow row = rowByEntryId.get(Long.toString(entryId));
            if (row == null) {
                throw badRequest("部分知识条目不存在、已完善或没有有效文件");
            }
            orderedRows.add(row);
        }
        return List.copyOf(orderedRows);
    }

    private List<KnowledgeSupplementTemplateRow> queryByFilter(
            KnowledgeSupplementTemplateExportRequest request) {
        List<KnowledgeSupplementTemplateRow> rows = queryService.queryByFilter(request, FILTER_QUERY_LIMIT);
        if (rows.size() > MAX_EXPORT_ROWS) {
            throw badRequest("可导出记录超过5000条，请缩小筛选范围");
        }
        return rows;
    }

    private void validateRequest(KnowledgeSupplementTemplateExportRequest request) {
        if (request == null) {
            throw badRequest("请求体不能为空");
        }
        if (request.exactMode() && request.hasFilterCondition()) {
            throw badRequest("entryIds不能与筛选条件同时使用");
        }
        if (request.exactMode()
                && new HashSet<>(request.entryIds()).size() != request.entryIds().size()) {
            throw badRequest("知识条目ID不能重复");
        }
        if (request.entryType() != null && !KnowledgeEntryType.isValid(request.entryType())) {
            throw badRequest("知识条目类型不正确");
        }
        if (request.uploadStartTime() != null && request.uploadEndTime() != null
                && request.uploadStartTime().isAfter(request.uploadEndTime())) {
            throw badRequest("上传开始时间不能晚于结束时间");
        }
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(CommonErrorCode.BAD_REQUEST, message);
    }
}
