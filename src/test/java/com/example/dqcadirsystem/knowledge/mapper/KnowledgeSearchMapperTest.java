package com.example.dqcadirsystem.knowledge.mapper;

import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeSearchRequest;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeSearchRow;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 使用 H2 真实执行检索SQL，验证检索范围、状态约束、分页和当前文件选择。 */
@MybatisTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:knowledge_search;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "mybatis.mapper-locations=classpath*:mapper/**/*.xml",
        "mybatis.configuration.map-underscore-to-camel-case=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("/sql/knowledge-page-test.sql")
class KnowledgeSearchMapperTest {

    @Autowired
    private KnowledgeEntryMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 标题、关键词等任一字段匹配时应返回完整卡片投影和字符串ID。 */
    @Test
    void shouldSearchCompletedEntryByKeyword() {
        KnowledgeSearchRequest request = new KnowledgeSearchRequest("暖通", "DRAWING", 1, 10);

        List<KnowledgeSearchRow> rows = mapper.selectSearch(request);

        assertEquals(1, mapper.countSearch(request));
        assertEquals(1, rows.size());
        assertEquals("2100000000000000001", rows.getFirst().entryId());
        assertEquals("2200000000000000001", rows.getFirst().fileId());
        assertEquals("总部办公楼暖通空调设计图1.pdf", rows.getFirst().originalFileName());
    }

    /** 待补充条目即使文件和关键词均匹配，也不能进入面向用户的检索结果。 */
    @Test
    void shouldExcludePendingInformationEntry() {
        insertEntry(2100000000000000010L, 0, "待补充暖通资料");
        insertFile(2200000000000000010L, 2100000000000000010L,
                "待补充暖通资料.pdf", "success", 1, 1, "2026-07-01 10:00:00");
        KnowledgeSearchRequest request = new KnowledgeSearchRequest("待补充", null, 1, 10);

        assertEquals(0, mapper.countSearch(request));
        assertTrue(mapper.selectSearch(request).isEmpty());
    }

    /** 失败、历史和删除文件均不可访问，不能作为检索卡片。 */
    @Test
    void shouldExcludeEntryWithoutCurrentSuccessfulFile() {
        insertEntry(2100000000000000010L, 1, "失败文件资料");
        insertFile(2200000000000000010L, 2100000000000000010L,
                "失败文件资料.pdf", "failed", 1, 1, "2026-07-01 10:00:00");
        KnowledgeSearchRequest request = new KnowledgeSearchRequest("失败文件", null, 1, 10);

        assertEquals(0, mapper.countSearch(request));
        assertTrue(mapper.selectSearch(request).isEmpty());
    }

    /** 异常存在多个当前文件时只选择上传时间最新的一条，并保持计数为一个条目。 */
    @Test
    void shouldChooseLatestWhenMultipleCurrentFilesExist() {
        insertFile(2200000000000000011L, 2100000000000000001L,
                "最新暖通文件.pdf", "success", 1, 1, "2026-07-01 12:00:00");
        KnowledgeSearchRequest request = new KnowledgeSearchRequest("暖通", null, 1, 10);

        List<KnowledgeSearchRow> rows = mapper.selectSearch(request);

        assertEquals(1, mapper.countSearch(request));
        assertEquals(1, rows.size());
        assertEquals("2200000000000000011", rows.getFirst().fileId());
    }

    private void insertEntry(long id, int infoStatus, String title) {
        jdbcTemplate.update("""
                INSERT INTO knowledge_entry (
                    id, entry_type, entry_code, title, version, info_status, status,
                    delete_marker, created_at, updated_at
                ) VALUES (?, 'DRAWING', ?, ?, 'V1.0', ?, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id, "CODE-" + id, title, infoStatus);
    }

    private void insertFile(
            long id, long entryId, String name, String uploadStatus,
            int current, int status, String uploadedAt) {
        jdbcTemplate.update("""
                INSERT INTO knowledge_file (
                    id, entry_id, original_file_name, file_ext, file_size, file_url,
                    upload_status, is_current, uploaded_at, status
                ) VALUES (?, ?, ?, 'pdf', 100, 'https://bucket/file.pdf', ?, ?, ?, ?)
                """, id, entryId, name, uploadStatus, current, uploadedAt, status);
    }
}
