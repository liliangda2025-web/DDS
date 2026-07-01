package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryInfoImportFailureResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryInfoImportResponse;
import com.example.dqcadirsystem.knowledge.enums.KnowledgeEntryType;
import com.example.dqcadirsystem.knowledge.service.model.KnowledgeEntryInfoImportRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * 批量导入知识条目信息的编排服务。
 *
 * <p>解析和整表重复检查在事务外完成，随后按 Excel 顺序逐行调用独立事务。服务本身不能开启大事务，
 * 否则一行失败会回滚其他行，或让大量行锁一直持有到整个文件处理结束。</p>
 */
@Service
public class KnowledgeEntryInfoImportService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeEntryInfoImportService.class);

    private final KnowledgeEntryInfoExcelParser excelParser;
    private final KnowledgeEntryInfoImportTransactionService transactionService;

    public KnowledgeEntryInfoImportService(
            KnowledgeEntryInfoExcelParser excelParser,
            KnowledgeEntryInfoImportTransactionService transactionService) {
        this.excelParser = excelParser;
        this.transactionService = transactionService;
    }

    /** 解析完整工作簿，收集预校验错误，并逐行回填数据库。 */
    public KnowledgeEntryInfoImportResponse importInfo(MultipartFile file) {
        List<KnowledgeEntryInfoImportRow> rows = excelParser.parse(file);
        Map<Integer, String> failureByRow = preValidate(rows);
        List<KnowledgeEntryInfoImportFailureResponse> failedList = new ArrayList<>();
        int successCount = 0;

        for (KnowledgeEntryInfoImportRow row : rows) {
            String preValidationError = failureByRow.get(row.rowNum());
            if (preValidationError != null) {
                failedList.add(KnowledgeEntryInfoImportFailureResponse.from(row, preValidationError));
                continue;
            }
            try {
                transactionService.importRow(row);
                successCount++;
            } catch (KnowledgeEntryInfoImportRowException exception) {
                failedList.add(KnowledgeEntryInfoImportFailureResponse.from(row, exception.getMessage()));
            } catch (RuntimeException exception) {
                // 未知单行故障完整记录，但不能把 SQL 或实现细节暴露给前端，也不能阻断后续正常行。
                log.error("Unexpected knowledge info import row failure: row={}, entryId={}, fileId={}",
                        row.rowNum(), row.entryIdText(), row.fileIdText(), exception);
                failedList.add(KnowledgeEntryInfoImportFailureResponse.from(
                        row, "该行处理失败，请稍后重试"));
            }
        }
        return KnowledgeEntryInfoImportResponse.of(rows.size(), successCount, failedList);
    }

    /**
     * 完整扫描后再判断重复，冲突中的所有行都会失败，不会产生“第一行因顺序先到而成功”的随机结果。
     */
    private Map<Integer, String> preValidate(List<KnowledgeEntryInfoImportRow> rows) {
        Map<Integer, String> failures = new LinkedHashMap<>();
        for (KnowledgeEntryInfoImportRow row : rows) {
            String error = validateRow(row);
            if (error != null) {
                failures.put(row.rowNum(), error);
            }
        }

        markDuplicates(rows, KnowledgeEntryInfoImportRow::entryId,
                "knowledge_entry_id在Excel中重复", failures);
        markDuplicates(rows, KnowledgeEntryInfoImportRow::fileId,
                "knowledge_file_id在Excel中重复", failures);
        markDuplicates(rows, this::businessKey,
                "Excel中存在重复的知识条目类型、条目编码和版本", failures);
        return failures;
    }

    /** 按接口字段顺序进行确定性校验，只返回当前行第一条错误。 */
    private String validateRow(KnowledgeEntryInfoImportRow row) {
        if (row.parseError() != null) {
            return row.parseError();
        }
        if (row.fileName() == null) {
            return "文件名称不能为空";
        }
        if (row.fileType() == null) {
            return "文件格式不能为空";
        }
        if (row.uploadStatus() == null) {
            return "上传状态不能为空";
        }
        if (row.entryType() == null) {
            return "知识条目类型不能为空";
        }
        if (!KnowledgeEntryType.isValid(row.entryType())) {
            return "知识条目类型不合法";
        }
        if (row.entryCode() == null) {
            return "文件编号 / 图纸编号不能为空";
        }
        if (row.entryCode().equalsIgnoreCase("TMP_" + row.entryIdText())) {
            return "文件编号 / 图纸编号必须替换临时值";
        }
        if (row.title() == null) {
            return "标题不能为空";
        }
        if (row.version() == null) {
            return "版本不能为空";
        }
        if ("TEMP".equalsIgnoreCase(row.version())) {
            return "版本必须替换临时值TEMP";
        }

        String lengthError = firstLengthError(row);
        return lengthError;
    }

    private String firstLengthError(KnowledgeEntryInfoImportRow row) {
        if (tooLong(row.entryType(), 50)) return "知识条目类型长度不能超过50个字符";
        if (tooLong(row.entryCode(), 100)) return "条目编码长度不能超过100个字符";
        if (tooLong(row.title(), 255)) return "标题长度不能超过255个字符";
        if (tooLong(row.keywords(), 500)) return "关键词长度不能超过500个字符";
        if (tooLong(row.version(), 50)) return "版本长度不能超过50个字符";
        if (tooLong(row.projectName(), 255)) return "所属项目长度不能超过255个字符";
        if (tooLong(row.systemSource(), 255)) return "系统来源长度不能超过255个字符";
        if (tooLong(row.professionCode(), 100)) return "专业代码长度不能超过100个字符";
        if (tooLong(row.authorName(), 100)) return "编写人长度不能超过100个字符";
        if (tooLong(row.secretLevel(), 50)) return "密级长度不能超过50个字符";
        return null;
    }

    private boolean tooLong(String value, int maxLength) {
        return value != null && value.length() > maxLength;
    }

    private String businessKey(KnowledgeEntryInfoImportRow row) {
        if (row.entryType() == null || row.entryCode() == null || row.version() == null) {
            return null;
        }
        // MySQL 常用的不区分大小写排序规则会把大小写不同的文本视为同一唯一键，
        // 预检查同步转为小写，避免两个冲突行因执行顺序不同而只让后一行失败。
        return (row.entryType() + '\u0000' + row.entryCode() + '\u0000' + row.version())
                .toLowerCase(Locale.ROOT);
    }

    private <T> void markDuplicates(
            List<KnowledgeEntryInfoImportRow> rows,
            Function<KnowledgeEntryInfoImportRow, T> keyExtractor,
            String reason,
            Map<Integer, String> failures) {
        Map<T, List<KnowledgeEntryInfoImportRow>> grouped = new HashMap<>();
        for (KnowledgeEntryInfoImportRow row : rows) {
            T key = keyExtractor.apply(row);
            if (key != null) {
                grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
            }
        }
        grouped.values().stream()
                .filter(group -> group.size() > 1)
                .flatMap(List::stream)
                .forEach(row -> failures.putIfAbsent(row.rowNum(), reason));
    }
}
