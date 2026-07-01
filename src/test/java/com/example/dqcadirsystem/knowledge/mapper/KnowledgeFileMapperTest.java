package com.example.dqcadirsystem.knowledge.mapper;

import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeFileRow;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeFilePreviewRow;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 使用 H2 MySQL 兼容模式验证文件替换 Mapper 的真实 SQL。 */
@MybatisTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:knowledge_file;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "mybatis.mapper-locations=classpath*:mapper/**/*.xml",
        "mybatis.configuration.map-underscore-to-camel-case=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("/sql/knowledge-page-test.sql")
class KnowledgeFileMapperTest {

    @Autowired
    private KnowledgeFileMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 新文件插入前应把旧当前文件全部降级，并正确返回 19 位字符串 ID。 */
    @Test
    void shouldReplaceCurrentFile() {
        long entryId = 2100000000000000001L;
        long fileId = 2200000000000000099L;

        assertEquals(entryId, mapper.selectActiveEntryId(entryId));
        assertEquals(entryId, mapper.selectActiveEntryIdForUpdate(entryId));
        assertEquals(1, mapper.unsetCurrentFiles(entryId));
        assertEquals(1, mapper.insertSuccessfulFile(
                fileId, entryId, "new.dwg", "dwg", 4096L,
                "https://bucket/knowledge/entry/new.dwg"));

        KnowledgeFileRow row = mapper.selectById(fileId);
        assertEquals(Long.toString(fileId), row.fileId());
        assertEquals(Long.toString(entryId), row.entryId());
        assertEquals("success", row.uploadStatus());
        assertEquals(1, row.isCurrent());
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT is_current FROM knowledge_file WHERE id = 2200000000000000001",
                Integer.class));
    }

    /** 已逻辑删除条目既不能通过预检查，也不能在事务内被锁定。 */
    @Test
    void shouldNotFindDeletedEntry() {
        assertNull(mapper.selectActiveEntryId(2100000000000000003L));
        assertNull(mapper.selectActiveEntryIdForUpdate(2100000000000000003L));
    }

    /** 降级操作会修复同一条目意外存在多条当前文件的历史数据。 */
    @Test
    void shouldDemoteAllCurrentFiles() {
        long entryId = 2100000000000000001L;
        jdbcTemplate.update("""
                INSERT INTO knowledge_file (
                    id, entry_id, original_file_name, file_ext, file_size, file_url,
                    upload_status, is_current, uploaded_at, status
                ) VALUES (?, ?, 'duplicate.pdf', 'pdf', 1, 'url', 'success', 1, CURRENT_TIMESTAMP, 1)
                """, 2200000000000000002L, entryId);

        assertEquals(2, mapper.unsetCurrentFiles(entryId));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_file WHERE entry_id = ? AND is_current = 1",
                Integer.class, entryId));
    }

    /** 当前正常文件且所属条目正常时，才能按 fileId 取得预览定位信息。 */
    @Test
    void shouldSelectCurrentFileForPreview() {
        KnowledgeFilePreviewRow row = mapper.selectCurrentPreviewById(2200000000000000001L);

        assertEquals("2200000000000000001", row.fileId());
        assertEquals("2100000000000000001", row.entryId());
        assertEquals("pdf", row.fileType());
        assertEquals(
                "https://liliangda-oss-test.oss-cn-beijing.aliyuncs.com/knowledge/2100000000000000001/2200000000000000001.pdf",
                row.fileUrl());
    }

    /** 替换后降级为历史文件的记录不能继续通过统一预览入口访问。 */
    @Test
    void shouldNotPreviewHistoricalFile() {
        jdbcTemplate.update("UPDATE knowledge_file SET is_current = 0 WHERE id = ?",
                2200000000000000001L);

        assertNull(mapper.selectCurrentPreviewById(2200000000000000001L));
    }

    /** 已逻辑删除的文件即使仍保留当前标记，也不能继续预览。 */
    @Test
    void shouldNotPreviewDeletedFile() {
        jdbcTemplate.update("UPDATE knowledge_file SET status = 0 WHERE id = ?",
                2200000000000000001L);

        assertNull(mapper.selectCurrentPreviewById(2200000000000000001L));
    }

    /** 文件正常但所属条目已删除时也必须拒绝预览。 */
    @Test
    void shouldNotPreviewFileOfDeletedEntry() {
        jdbcTemplate.update("UPDATE knowledge_entry SET status = 0 WHERE id = ?",
                2100000000000000001L);

        assertNull(mapper.selectCurrentPreviewById(2200000000000000001L));
    }
}
