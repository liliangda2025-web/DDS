package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.knowledge.enums.KnowledgeEntryType;
import com.example.dqcadirsystem.knowledge.enums.KnowledgeFileType;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeFileRow;
import com.example.dqcadirsystem.knowledge.storage.StoredObject;
import com.example.dqcadirsystem.knowledge.validation.ValidatedKnowledgeFile;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 使用 H2 验证单文件占位条目与文件记录的独立事务原子性。 */
@MybatisTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:knowledge_batch_persistence;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "mybatis.mapper-locations=classpath*:mapper/**/*.xml",
        "mybatis.configuration.map-underscore-to-camel-case=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(KnowledgeBatchUploadPersistenceService.class)
@Sql("/sql/knowledge-page-test.sql")
class KnowledgeBatchUploadPersistenceServiceTest {

    @Autowired
    private KnowledgeBatchUploadPersistenceService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistExactPlaceholderAndFileValues() {
        long entryId = 2300000000000000001L;
        long fileId = 2400000000000000001L;
        ValidatedKnowledgeFile file =
                new ValidatedKnowledgeFile("设备说明.pdf", KnowledgeFileType.PDF, 100L);
        StoredObject stored = new StoredObject(
                "knowledge/" + entryId + "/" + fileId + ".pdf",
                "https://bucket/knowledge/" + entryId + "/" + fileId + ".pdf");

        KnowledgeFileRow row = service.createPlaceholderEntryAndFile(
                entryId, fileId, KnowledgeEntryType.PROGRAM_RULE, "设备说明", file, stored);

        assertEquals(Long.toString(fileId), row.fileId());
        assertEquals("TMP_" + entryId, jdbcTemplate.queryForObject(
                "SELECT entry_code FROM knowledge_entry WHERE id = ?", String.class, entryId));
        assertEquals("TEMP", jdbcTemplate.queryForObject(
                "SELECT version FROM knowledge_entry WHERE id = ?", String.class, entryId));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT info_status FROM knowledge_entry WHERE id = ?", Integer.class, entryId));
        assertNull(jdbcTemplate.queryForObject(
                "SELECT created_by FROM knowledge_entry WHERE id = ?", Long.class, entryId));
        assertNull(jdbcTemplate.queryForObject(
                "SELECT uploaded_by FROM knowledge_file WHERE id = ?", Long.class, fileId));
    }

    /** 文件插入失败时，先插入的占位条目也必须随同一事务回滚。 */
    @Test
    void shouldRollbackPlaceholderWhenFileInsertFails() {
        long entryId = 2300000000000000002L;
        long fileId = 2400000000000000002L;
        ValidatedKnowledgeFile file =
                new ValidatedKnowledgeFile(null, KnowledgeFileType.PDF, 10L);
        StoredObject stored = new StoredObject("knowledge/key", "https://bucket/key");

        assertThrows(DataIntegrityViolationException.class,
                () -> service.createPlaceholderEntryAndFile(
                        entryId, fileId, KnowledgeEntryType.DRAWING,
                        "duplicate", file, stored));

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_entry WHERE id = ?", Integer.class, entryId));
    }
}
