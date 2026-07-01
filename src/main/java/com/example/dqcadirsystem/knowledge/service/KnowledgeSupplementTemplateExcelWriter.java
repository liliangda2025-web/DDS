package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.knowledge.enums.KnowledgeEntryType;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeSupplementTemplateRow;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 使用 Apache POI 生成知识条目信息补充模板。
 *
 * <p>系统识别列和用户填写列通过颜色与锁定状态清晰区分。工作表保护只用于降低误修改概率，
 * 并不是安全边界；批量导入接口仍必须重新校验 ID、文件关系和全部业务字段。</p>
 */
@Component
public class KnowledgeSupplementTemplateExcelWriter {

    public static final String SHEET_NAME = "知识条目信息补充模板";
    public static final int SYSTEM_COLUMN_COUNT = 7;
    public static final int TOTAL_COLUMN_COUNT = 18;

    /** 数据验证覆盖最大 5000 行，与单次筛选导出上限一致。 */
    private static final int MAX_DATA_ROW = 5000;
    private static final String SHEET_PROTECTION_PASSWORD = "dqcadirsystem-template";
    private static final String FONT_NAME = "Microsoft YaHei";

    private static final List<String> HEADERS = List.of(
            "knowledge_entry_id", "knowledge_file_id", "文件名称", "文件格式", "文件大小", "上传时间", "上传状态",
            "知识条目类型", "文件编号 / 图纸编号", "标题", "关键词", "版本", "所属项目", "发版日期", "系统来源",
            "专业代码", "编写人", "密级");

    /** 四个最低完整性字段在表头中使用单独颜色提示。 */
    private static final List<Integer> REQUIRED_EDITABLE_COLUMNS = List.of(7, 8, 9, 11);

    /**
     * 完整构建工作簿并返回字节数组；只有本方法成功返回后 Controller 才会发送二进制响应。
     */
    public byte[] write(List<KnowledgeSupplementTemplateRow> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(SHEET_NAME);
            sheet.setDisplayGridlines(false);
            sheet.createFreezePane(0, 1);

            Styles styles = createStyles(workbook);
            writeHeader(sheet.createRow(0), styles);
            for (int index = 0; index < rows.size(); index++) {
                writeDataRow(sheet.createRow(index + 1), rows.get(index), styles);
            }

            configureColumns(sheet);
            sheet.setAutoFilter(new CellRangeAddress(0, rows.size(), 0, TOTAL_COLUMN_COUNT - 1));
            addEntryTypeValidation(sheet);
            sheet.protectSheet(SHEET_PROTECTION_PASSWORD);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("生成知识条目信息补充模板失败", exception);
        }
    }

    /** 表头使用两组颜色区分系统字段和可填写字段，必填字段再使用橙色强调。 */
    private void writeHeader(Row row, Styles styles) {
        row.setHeightInPoints(28);
        for (int column = 0; column < HEADERS.size(); column++) {
            Cell cell = row.createCell(column);
            cell.setCellValue(HEADERS.get(column));
            if (column < SYSTEM_COLUMN_COUNT) {
                cell.setCellStyle(styles.systemHeader());
            } else if (REQUIRED_EDITABLE_COLUMNS.contains(column)) {
                cell.setCellStyle(styles.requiredHeader());
            } else {
                cell.setCellStyle(styles.editableHeader());
            }
        }
    }

    /**
     * 按固定列序写入一行。ID 始终写成字符串；数字和日期使用真实 Excel 类型，便于排序和筛选。
     */
    private void writeDataRow(Row row, KnowledgeSupplementTemplateRow data, Styles styles) {
        row.setHeightInPoints(22);
        writeText(row, 0, data.entryId(), styles.systemText());
        writeText(row, 1, data.fileId(), styles.systemText());
        writeText(row, 2, data.originalFileName(), styles.systemText());
        writeText(row, 3, data.fileType(), styles.systemText());
        writeNumber(row, 4, data.fileSize(), styles.systemNumber());
        writeDateTime(row, 5, data.uploadedAt(), styles.systemDateTime());
        writeText(row, 6, data.uploadStatus(), styles.systemText());

        writeText(row, 7, data.entryType(), styles.requiredEditable());
        writeText(row, 8, data.entryCode(), styles.requiredEditable());
        writeText(row, 9, data.title(), styles.requiredEditable());
        writeText(row, 10, data.keywords(), styles.editableText());
        writeText(row, 11, data.version(), styles.requiredEditable());
        writeText(row, 12, data.projectName(), styles.editableText());
        writeDate(row, 13, data.releaseDate(), styles.editableDate());
        writeText(row, 14, data.systemSource(), styles.editableText());
        writeText(row, 15, data.professionCode(), styles.editableText());
        writeText(row, 16, data.authorName(), styles.editableText());
        writeText(row, 17, data.secretLevel(), styles.editableText());
    }

    private void writeText(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellStyle(style);
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private void writeNumber(Row row, int column, Long value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellStyle(style);
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private void writeDateTime(Row row, int column, java.time.LocalDateTime value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellStyle(style);
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private void writeDate(Row row, int column, java.time.LocalDate value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellStyle(style);
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    /** 固定列宽比自动扫描 5000 行更稳定，也避免异常长文本把列撑得不可读。 */
    private void configureColumns(org.apache.poi.ss.usermodel.Sheet sheet) {
        int[] widths = {24, 24, 34, 12, 14, 22, 14, 18, 28, 30, 30, 16, 24, 16, 22, 18, 16, 14};
        for (int column = 0; column < widths.length; column++) {
            sheet.setColumnWidth(column, widths[column] * 256);
        }
    }

    /** 为全部可导出数据行提供固定业务枚举下拉选择，降低手工输入拼写错误。 */
    private void addEntryTypeValidation(org.apache.poi.ss.usermodel.Sheet sheet) {
        String[] values = Arrays.stream(KnowledgeEntryType.values())
                .map(KnowledgeEntryType::value)
                .toArray(String[]::new);
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(values);
        CellRangeAddressList range = new CellRangeAddressList(1, MAX_DATA_ROW, 7, 7);
        DataValidation validation = helper.createValidation(constraint, range);
        validation.setEmptyCellAllowed(false);
        validation.setShowErrorBox(true);
        validation.createErrorBox("知识条目类型错误", "请选择下拉列表中的知识条目类型");
        sheet.addValidationData(validation);
    }

    /** 集中创建并复用样式，避免每个单元格产生独立样式导致工作簿膨胀。 */
    private Styles createStyles(XSSFWorkbook workbook) {
        CellStyle systemHeader = headerStyle(workbook, IndexedColors.GREY_80_PERCENT, IndexedColors.WHITE);
        CellStyle editableHeader = headerStyle(workbook, IndexedColors.DARK_BLUE, IndexedColors.WHITE);
        CellStyle requiredHeader = headerStyle(workbook, IndexedColors.ORANGE, IndexedColors.BLACK);

        CellStyle systemText = bodyStyle(workbook, true, IndexedColors.GREY_25_PERCENT);
        CellStyle systemNumber = cloneWithFormat(workbook, systemText, "#,##0");
        CellStyle systemDateTime = cloneWithFormat(workbook, systemText, "yyyy-mm-dd hh:mm:ss");
        CellStyle editableText = bodyStyle(workbook, false, IndexedColors.WHITE);
        CellStyle requiredEditable = bodyStyle(workbook, false, IndexedColors.LIGHT_YELLOW);
        CellStyle editableDate = cloneWithFormat(workbook, editableText, "yyyy-mm-dd");
        return new Styles(systemHeader, editableHeader, requiredHeader, systemText, systemNumber,
                systemDateTime, editableText, requiredEditable, editableDate);
    }

    private CellStyle headerStyle(XSSFWorkbook workbook, IndexedColors fill, IndexedColors fontColor) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(fill.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setLocked(true);
        style.setBorderBottom(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setBold(true);
        font.setColor(fontColor.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle bodyStyle(XSSFWorkbook workbook, boolean locked, IndexedColors fill) {
        CellStyle style = workbook.createCellStyle();
        style.setLocked(locked);
        style.setFillForegroundColor(fill.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.HAIR);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        style.setFont(font);
        return style;
    }

    private CellStyle cloneWithFormat(XSSFWorkbook workbook, CellStyle source, String format) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(source);
        style.setDataFormat(workbook.createDataFormat().getFormat(format));
        return style;
    }

    private record Styles(
            CellStyle systemHeader,
            CellStyle editableHeader,
            CellStyle requiredHeader,
            CellStyle systemText,
            CellStyle systemNumber,
            CellStyle systemDateTime,
            CellStyle editableText,
            CellStyle requiredEditable,
            CellStyle editableDate) {
    }
}
