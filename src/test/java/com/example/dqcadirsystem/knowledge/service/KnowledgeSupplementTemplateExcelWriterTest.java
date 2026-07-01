package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeSupplementTemplateRow;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 重新读取生成的 XLSX，验证内容、类型、格式、锁定和数据校验。 */
class KnowledgeSupplementTemplateExcelWriterTest {

    private final KnowledgeSupplementTemplateExcelWriter writer =
            new KnowledgeSupplementTemplateExcelWriter();

    @Test
    void shouldGenerateReadableProtectedTemplateWithoutFileUrl() throws Exception {
        KnowledgeSupplementTemplateRow data = new KnowledgeSupplementTemplateRow(
                "2100000000000000009", "2200000000000000009", "暖通图纸.pdf", "pdf", 1331200L,
                LocalDateTime.of(2026, 7, 1, 10, 0), "success", "DRAWING",
                "TMP_2100000000000000009", "暖通图纸", "暖通 空调", "TEMP", "总部项目",
                LocalDate.of(2026, 6, 30), "图纸库", "HVAC", "张工", "内部");

        byte[] bytes = writer.write(List.of(data));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet(KnowledgeSupplementTemplateExcelWriter.SHEET_NAME);
            assertNotNull(sheet);
            assertEquals(1, sheet.getLastRowNum());
            assertEquals("knowledge_entry_id", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("密级", sheet.getRow(0).getCell(17).getStringCellValue());
            assertTrue(java.util.stream.IntStream.range(0, 18)
                    .mapToObj(index -> sheet.getRow(0).getCell(index).getStringCellValue())
                    .noneMatch(header -> header.contains("地址") || header.contains("fileUrl")));

            assertEquals(CellType.STRING, sheet.getRow(1).getCell(0).getCellType());
            assertEquals("2100000000000000009", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals(CellType.NUMERIC, sheet.getRow(1).getCell(4).getCellType());
            assertEquals("#,##0", sheet.getRow(1).getCell(4).getCellStyle().getDataFormatString());
            assertEquals("yyyy-mm-dd hh:mm:ss",
                    sheet.getRow(1).getCell(5).getCellStyle().getDataFormatString());
            assertEquals("yyyy-mm-dd", sheet.getRow(1).getCell(13).getCellStyle().getDataFormatString());

            assertTrue(sheet.getProtect());
            assertTrue(sheet.getRow(1).getCell(0).getCellStyle().getLocked());
            assertFalse(sheet.getRow(1).getCell(7).getCellStyle().getLocked());
            assertFalse(sheet.getRow(1).getCell(17).getCellStyle().getLocked());
            assertEquals(1, sheet.getDataValidations().size());
            assertTrue(sheet.getDataValidations().getFirst()
                    .getValidationConstraint().getExplicitListValues().length >= 4);
            // POI 的 horizontalSplitPosition 表示横向分隔线所在的行位置；冻结首行因此为 1。
            assertEquals(1, sheet.getPaneInformation().getHorizontalSplitPosition());
            assertEquals(0, sheet.getPaneInformation().getVerticalSplitPosition());
            assertTrue(sheet.getColumnWidth(2) > sheet.getColumnWidth(3));
            assertTrue(sheet.getCTWorksheet().isSetAutoFilter());
        }
    }
}
