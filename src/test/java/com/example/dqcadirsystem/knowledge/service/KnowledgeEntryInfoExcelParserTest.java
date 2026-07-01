package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeSupplementTemplateRow;
import com.example.dqcadirsystem.knowledge.service.model.KnowledgeEntryInfoImportRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 验证导出模板可以被安全解析，同时拒绝结构错误和容易丢失精度的单元格。 */
class KnowledgeEntryInfoExcelParserTest {

    private final KnowledgeSupplementTemplateExcelWriter writer =
            new KnowledgeSupplementTemplateExcelWriter();
    private final KnowledgeEntryInfoExcelParser parser = new KnowledgeEntryInfoExcelParser();

    @Test
    void shouldParseWorkbookGeneratedByExportWriter() {
        List<KnowledgeEntryInfoImportRow> rows = parser.parse(file(validWorkbook()));

        KnowledgeEntryInfoImportRow row = rows.getFirst();
        assertEquals(2, row.rowNum());
        assertEquals(2100000000000000010L, row.entryId());
        assertEquals(2200000000000000010L, row.fileId());
        assertEquals("DRAWING", row.entryType());
        assertEquals("DWG-001", row.entryCode());
        assertEquals(LocalDate.of(2026, 7, 1), row.releaseDate());
        assertNull(row.parseError());
    }

    @Test
    void shouldRecordFormulaAsRowFailureWithoutRejectingWholeWorkbook() throws Exception {
        byte[] bytes = modify(validWorkbook(), workbook ->
                workbook.getSheetAt(0).getRow(1).getCell(9).setCellFormula("1+1"));

        KnowledgeEntryInfoImportRow row = parser.parse(file(bytes)).getFirst();

        assertEquals("不支持公式单元格", row.parseError());
    }

    @Test
    void shouldRejectNumericLongIdToPreventPrecisionLoss() throws Exception {
        byte[] bytes = modify(validWorkbook(), workbook ->
                workbook.getSheetAt(0).getRow(1).getCell(0).setCellValue(2100000000000000010D));

        KnowledgeEntryInfoImportRow row = parser.parse(file(bytes)).getFirst();

        assertEquals("knowledge_entry_id必须使用文本格式，避免19位ID精度丢失", row.parseError());
        assertNull(row.entryId());
    }

    @Test
    void shouldRejectFakeXlsxFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "template.xlsx", "application/octet-stream", "not-xlsx".getBytes());

        BusinessException exception = assertThrows(BusinessException.class, () -> parser.parse(file));

        assertEquals("Excel文件损坏、加密或格式不正确", exception.getMessage());
    }

    @Test
    void shouldRejectFileLargerThanTenMebibytesBeforeParsing() {
        MockMultipartFile oversized = new MockMultipartFile(
                "file", "template.xlsx", "application/octet-stream",
                new byte[(int) KnowledgeEntryInfoExcelParser.MAX_FILE_SIZE + 1]);

        BusinessException exception = assertThrows(BusinessException.class, () -> parser.parse(oversized));

        assertEquals("Excel文件大小不能超过10 MiB", exception.getMessage());
    }

    private byte[] validWorkbook() {
        KnowledgeSupplementTemplateRow row = new KnowledgeSupplementTemplateRow(
                "2100000000000000010", "2200000000000000010", "暖通图纸.pdf", "pdf", 100L,
                LocalDateTime.of(2026, 7, 1, 10, 0), "success", "DRAWING", "DWG-001",
                "正式暖通图纸", "暖通 空调", "V1.0", "总部项目", LocalDate.of(2026, 7, 1),
                "图纸库", "HVAC", "张工", "内部");
        return writer.write(List.of(row));
    }

    private MockMultipartFile file(byte[] bytes) {
        return new MockMultipartFile(
                "file", "knowledge_supplement_template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
    }

    private byte[] modify(byte[] source, WorkbookChange change) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(source));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            change.apply(workbook);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    @FunctionalInterface
    private interface WorkbookChange {
        void apply(XSSFWorkbook workbook);
    }
}
