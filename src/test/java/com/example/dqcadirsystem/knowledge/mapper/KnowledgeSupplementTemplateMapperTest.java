package com.example.dqcadirsystem.knowledge.mapper;

import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeSupplementTemplateExportRequest;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeSupplementTemplateRow;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 使用 H2 验证补充模板只查询待补充条目和最新当前成功文件。 */
@MybatisTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:knowledge_supplement;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "mybatis.mapper-locations=classpath*:mapper/**/*.xml",
        "mybatis.configuration.map-underscore-to-camel-case=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("/sql/knowledge-page-test.sql")
class KnowledgeSupplementTemplateMapperTest {

    @Autowired
    private KnowledgeEntryMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 精确查询只接受待补充记录，并确定性选择最新当前成功文件。 */
    @Test
    void shouldSelectExactPendingEntryWithLatestCurrentFile() {
        long entryId = 2100000000000000010L;
        insertPendingEntry(entryId, "DRAWING", "TMP_" + entryId, "暖通图纸");
        insertFile(2200000000000000010L, entryId, "old.pdf", "success", 1,
                "2026-07-01 09:00:00", 1);
        insertFile(2200000000000000011L, entryId, "new.pdf", "success", 1,
                "2026-07-01 10:00:00", 1);

        List<KnowledgeSupplementTemplateRow> rows =
                mapper.selectPendingSupplementByEntryIds(List.of(entryId, 2100000000000000001L));

        assertEquals(1, rows.size());
        assertEquals(Long.toString(entryId), rows.getFirst().entryId());
        assertEquals("2200000000000000011", rows.getFirst().fileId());
        assertEquals("new.pdf", rows.getFirst().originalFileName());
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT info_status FROM knowledge_entry WHERE id = ?", Integer.class, entryId));
    }

    /** 筛选条件使用文件上传时间闭区间，并按上传时间倒序返回。 */
    @Test
    void shouldFilterByTypeNameAndClosedUploadTimeRange() {
        insertPendingEntry(2100000000000000010L, "DRAWING", "TMP_10", "暖通一");
        insertPendingEntry(2100000000000000011L, "DRAWING", "TMP_11", "暖通二");
        insertPendingEntry(2100000000000000012L, "LAW", "TMP_12", "法规");
        insertFile(2200000000000000010L, 2100000000000000010L, "暖通一.pdf", "success", 1,
                "2026-07-01 09:00:00", 1);
        insertFile(2200000000000000011L, 2100000000000000011L, "暖通二.pdf", "success", 1,
                "2026-07-01 10:00:00", 1);
        insertFile(2200000000000000012L, 2100000000000000012L, "暖通法规.pdf", "success", 1,
                "2026-07-01 09:30:00", 1);
        KnowledgeSupplementTemplateExportRequest request = new KnowledgeSupplementTemplateExportRequest(
                null, "DRAWING", "暖通",
                LocalDateTime.of(2026, 7, 1, 9, 0),
                LocalDateTime.of(2026, 7, 1, 10, 0));

        List<KnowledgeSupplementTemplateRow> rows =
                mapper.selectPendingSupplementByFilter(request, 5001);

        assertEquals(List.of("2100000000000000011", "2100000000000000010"),
                rows.stream().map(KnowledgeSupplementTemplateRow::entryId).toList());
    }

    /** 历史、失败或删除文件都不能作为补充模板的文件定位依据。 */
    @Test
    void shouldExcludeEntriesWithoutCurrentSuccessfulActiveFile() {
        insertPendingEntry(2100000000000000010L, "DRAWING", "TMP_10", "历史文件");
        insertPendingEntry(2100000000000000011L, "DRAWING", "TMP_11", "失败文件");
        insertPendingEntry(2100000000000000012L, "DRAWING", "TMP_12", "删除文件");
        insertFile(2200000000000000010L, 2100000000000000010L, "old.pdf", "success", 0,
                "2026-07-01 09:00:00", 1);
        insertFile(2200000000000000011L, 2100000000000000011L, "failed.pdf", "failed", 1,
                "2026-07-01 09:00:00", 1);
        insertFile(2200000000000000012L, 2100000000000000012L, "deleted.pdf", "success", 1,
                "2026-07-01 09:00:00", 0);

        assertTrue(mapper.selectPendingSupplementByFilter(
                new KnowledgeSupplementTemplateExportRequest(null, null, null, null, null), 5001).isEmpty());
    }

    private void insertPendingEntry(long id, String type, String code, String title) {
        jdbcTemplate.update("""
                INSERT INTO knowledge_entry (
                    id, entry_type, entry_code, title, version, info_status, status,
                    delete_marker, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'TEMP', 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id, type, code, title);
    }

    private void insertFile(
            long id, long entryId, String name, String uploadStatus,
            int current, String uploadedAt, int status) {
        jdbcTemplate.update("""
                INSERT INTO knowledge_file (
                    id, entry_id, original_file_name, file_ext, file_size, file_url,
                    upload_status, is_current, uploaded_at, status
                ) VALUES (?, ?, ?, 'pdf', 100, 'https://bucket/file.pdf', ?, ?, ?, ?)
                """, id, entryId, name, uploadStatus, current, uploadedAt, status);
    }
}
