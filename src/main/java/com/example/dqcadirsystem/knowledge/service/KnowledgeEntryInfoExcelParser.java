package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.knowledge.service.model.KnowledgeEntryInfoImportRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 解析系统导出的知识条目信息补充模板。
 *
 * <p>文件结构错误属于请求级失败；某个数据单元格格式错误只记录在对应行上，由业务层继续处理其他行。
 * 解析器不会计算公式，避免公式、外部链接或不同办公软件计算结果影响入库数据。</p>
 */
@Component
public class KnowledgeEntryInfoExcelParser {

    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    public static final int MAX_DATA_ROWS = 5000;

    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 校验文件协议并返回所有非空数据行。 */
    public List<KnowledgeEntryInfoImportRow> parse(MultipartFile file) {
        validateFile(file);
        try (InputStream inputStream = file.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheet(KnowledgeSupplementTemplateExcelWriter.SHEET_NAME);
            if (sheet == null) {
                throw badRequest("Excel缺少工作表：" + KnowledgeSupplementTemplateExcelWriter.SHEET_NAME);
            }
            validateHeader(sheet.getRow(0));
            return parseRows(sheet);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            // 损坏、加密、伪造扩展名等文件都不暴露 POI 的底层异常细节。
            throw badRequest("Excel文件损坏、加密或格式不正确");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("Excel文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw badRequest("Excel文件大小不能超过10 MiB");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw badRequest("只支持.xlsx格式的Excel文件");
        }
    }

    /** 表头必须与导出端共用的18列定义完全一致，并且不能额外增加有值的表头列。 */
    private void validateHeader(Row header) {
        if (header == null) {
            throw badRequest("Excel表头不正确");
        }
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        List<String> expected = KnowledgeSupplementTemplateExcelWriter.HEADERS;
        for (int column = 0; column < expected.size(); column++) {
            String actual = formatter.formatCellValue(header.getCell(column)).trim();
            if (!expected.get(column).equals(actual)) {
                throw badRequest("Excel表头不正确，请使用系统导出的补充模板");
            }
        }
        for (int column = expected.size(); column < header.getLastCellNum(); column++) {
            if (!formatter.formatCellValue(header.getCell(column)).isBlank()) {
                throw badRequest("Excel表头不正确，请使用系统导出的补充模板");
            }
        }
    }

    private List<KnowledgeEntryInfoImportRow> parseRows(Sheet sheet) {
        List<KnowledgeEntryInfoImportRow> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isEmptyRow(row, formatter)) {
                continue;
            }
            if (rows.size() >= MAX_DATA_ROWS) {
                throw badRequest("Excel数据不能超过5000行");
            }
            rows.add(parseRow(row, formatter));
        }
        if (rows.isEmpty()) {
            throw badRequest("Excel中没有可导入的数据");
        }
        return List.copyOf(rows);
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int column = 0; column < KnowledgeSupplementTemplateExcelWriter.TOTAL_COLUMN_COUNT; column++) {
            if (!formatter.formatCellValue(row.getCell(column)).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** 按模板固定列序读取数据，并保留第一个单元格解析错误。 */
    private KnowledgeEntryInfoImportRow parseRow(Row row, DataFormatter formatter) {
        ParseState state = new ParseState(row.getRowNum() + 1, formatter);
        String entryIdText = state.rawText(row.getCell(0));
        Long entryId = state.readId(row.getCell(0), "knowledge_entry_id");
        String fileIdText = state.rawText(row.getCell(1));
        Long fileId = state.readId(row.getCell(1), "knowledge_file_id");

        return new KnowledgeEntryInfoImportRow(
                state.rowNum(), entryIdText, entryId, fileIdText, fileId,
                state.readText(row.getCell(2)), state.readText(row.getCell(3)),
                state.readLong(row.getCell(4), "文件大小"),
                state.readDateTime(row.getCell(5), "上传时间"),
                state.readText(row.getCell(6)), state.readText(row.getCell(7)),
                state.readText(row.getCell(8)), state.readText(row.getCell(9)),
                state.readText(row.getCell(10)), state.readText(row.getCell(11)),
                state.readText(row.getCell(12)), state.readDate(row.getCell(13), "发版日期"),
                state.readText(row.getCell(14)), state.readText(row.getCell(15)),
                state.readText(row.getCell(16)), state.readText(row.getCell(17)), state.error());
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(CommonErrorCode.BAD_REQUEST, message);
    }

    /** 每行独立的解析状态，确保只返回第一条最靠左的错误提示。 */
    private static final class ParseState {
        private final int rowNum;
        private final DataFormatter formatter;
        private String error;

        private ParseState(int rowNum, DataFormatter formatter) {
            this.rowNum = rowNum;
            this.formatter = formatter;
        }

        private int rowNum() {
            return rowNum;
        }

        private String error() {
            return error;
        }

        private String rawText(Cell cell) {
            return cell == null ? null : trimToNull(formatter.formatCellValue(cell));
        }

        private String readText(Cell cell) {
            if (rejectFormula(cell)) {
                return null;
            }
            return rawText(cell);
        }

        private Long readId(Cell cell, String name) {
            if (rejectFormula(cell)) {
                return null;
            }
            if (cell == null || cell.getCellType() == CellType.BLANK) {
                fail(name + "不能为空");
                return null;
            }
            if (cell.getCellType() != CellType.STRING) {
                fail(name + "必须使用文本格式，避免19位ID精度丢失");
                return null;
            }
            String value = trimToNull(cell.getStringCellValue());
            if (value == null || !value.matches("[1-9]\\d*")) {
                fail(name + "格式不正确");
                return null;
            }
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException exception) {
                fail(name + "格式不正确");
                return null;
            }
        }

        private Long readLong(Cell cell, String name) {
            if (rejectFormula(cell)) {
                return null;
            }
            if (cell == null || cell.getCellType() != CellType.NUMERIC) {
                fail(name + "格式不正确");
                return null;
            }
            double value = cell.getNumericCellValue();
            if (!Double.isFinite(value) || value < 0 || value != Math.rint(value) || value > Long.MAX_VALUE) {
                fail(name + "格式不正确");
                return null;
            }
            return (long) value;
        }

        private LocalDateTime readDateTime(Cell cell, String name) {
            if (rejectFormula(cell)) {
                return null;
            }
            if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue();
            }
            String value = rawText(cell);
            if (value == null) {
                fail(name + "不能为空");
                return null;
            }
            try {
                return value.contains("T")
                        ? LocalDateTime.parse(value)
                        : LocalDateTime.parse(value, DISPLAY_DATE_TIME);
            } catch (DateTimeParseException exception) {
                fail(name + "格式不正确");
                return null;
            }
        }

        private LocalDate readDate(Cell cell, String name) {
            if (rejectFormula(cell)) {
                return null;
            }
            if (cell == null || cell.getCellType() == CellType.BLANK) {
                return null;
            }
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            String value = rawText(cell);
            if (value == null) {
                return null;
            }
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException exception) {
                fail(name + "格式不正确，应为yyyy-MM-dd");
                return null;
            }
        }

        private boolean rejectFormula(Cell cell) {
            if (cell != null && cell.getCellType() == CellType.FORMULA) {
                fail("不支持公式单元格");
                return true;
            }
            return false;
        }

        private void fail(String message) {
            if (error == null) {
                error = message;
            }
        }

        private static String trimToNull(String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
    }
}
