package com.example.dqcadirsystem.knowledge.mapper;

import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryCreateRequest;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryPageRequest;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryUpdateRequest;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeEntryDetailRow;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeEntryPageRow;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 使用 H2 MySQL 兼容模式执行真实 Mapper SQL，验证动态条件、关联和字段映射。
 */
@MybatisTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:knowledge_page;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "mybatis.mapper-locations=classpath*:mapper/**/*.xml",
        "mybatis.configuration.map-underscore-to-camel-case=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("/sql/knowledge-page-test.sql")
class KnowledgeEntryMapperTest {

    @Autowired
    private KnowledgeEntryMapper knowledgeEntryMapper;

    /**
     * 多个条件同时存在时只返回匹配条目，并正确映射 19 位字符串 ID 和当前文件信息。
     */
    @Test
    void shouldQueryPageWithAllFilters() {
        KnowledgeEntryPageRequest request = new KnowledgeEntryPageRequest(
                "DRAWING", "暖通", "DWG-HVAC", "空调", "HVAC", 1,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 1, 10);

        long total = knowledgeEntryMapper.countPage(request);
        List<KnowledgeEntryPageRow> rows = knowledgeEntryMapper.selectPage(request);

        assertEquals(1, total);
        assertEquals(1, rows.size());
        assertEquals("2100000000000000001", rows.getFirst().entryId());
        assertEquals("2200000000000000001", rows.getFirst().fileId());
        assertEquals("总部办公楼暖通空调设计图1.pdf", rows.getFirst().originalFileName());
    }

    /** 没有文件筛选条件时，左连接应保留没有当前文件的正常知识条目。 */
    @Test
    void shouldKeepEntryWithoutCurrentFile() {
        KnowledgeEntryPageRequest request = new KnowledgeEntryPageRequest(
                null, null, null, null, null, null, null, null, 1, 10);

        List<KnowledgeEntryPageRow> rows = knowledgeEntryMapper.selectPage(request);

        assertEquals(2, knowledgeEntryMapper.countPage(request));
        assertEquals(2, rows.size());
        assertEquals("2100000000000000002", rows.getFirst().entryId());
        assertNull(rows.getFirst().fileId());
    }

    /** 详情 SQL 应返回完整条目字段和当前有效文件，并保持 BIGINT ID 的字符串形式。 */
    @Test
    void shouldQueryEntryDetailWithCurrentFile() {
        KnowledgeEntryDetailRow row = knowledgeEntryMapper.selectDetail(2100000000000000001L);

        assertEquals("2100000000000000001", row.entryId());
        assertEquals("总部 暖通 空调", row.keywords());
        assertEquals("总部项目", row.projectName());
        assertEquals("2200000000000000001", row.fileId());
        assertEquals(2048000L, row.fileSize());
        assertEquals("success", row.uploadStatus());
    }

    /** LEFT JOIN 应保留无当前文件的正常条目，并把文件投影映射为 null。 */
    @Test
    void shouldQueryEntryDetailWithoutCurrentFile() {
        KnowledgeEntryDetailRow row = knowledgeEntryMapper.selectDetail(2100000000000000002L);

        assertEquals("2100000000000000002", row.entryId());
        assertEquals("CASE", row.entryType());
        assertNull(row.fileId());
        assertNull(row.originalFileName());
    }

    /** 已逻辑删除的条目不得被详情接口重新读取。 */
    @Test
    void shouldNotQueryLogicallyDeletedEntryDetail() {
        assertNull(knowledgeEntryMapper.selectDetail(2100000000000000003L));
    }

    /** 新增 SQL 应完整保存业务字段，并固定写入“已完善”和正常状态。 */
    @Test
    void shouldInsertKnowledgeEntry() {
        KnowledgeEntryCreateRequest request = new KnowledgeEntryCreateRequest(
                "LAW", "LAW-NEW-001", "新增法规", "新增 法规", "2026版",
                "法规库", LocalDate.of(2026, 6, 30), "法规库", "LAW", "法规管理员", "公开");

        assertEquals(0, knowledgeEntryMapper.countActiveByBusinessKey(request, null));
        assertEquals(1, knowledgeEntryMapper.insertEntry(2100000000000000099L, request));

        KnowledgeEntryDetailRow row = knowledgeEntryMapper.selectDetail(2100000000000000099L);
        assertEquals("2100000000000000099", row.entryId());
        assertEquals("新增法规", row.title());
        assertEquals("法规库", row.projectName());
        assertEquals(1, row.infoStatus());
        assertNull(row.fileId());
        assertEquals(1, knowledgeEntryMapper.countActiveByBusinessKey(request, null));
    }

    /** 业务键检查应识别现有正常记录。 */
    @Test
    void shouldCountExistingActiveBusinessKey() {
        KnowledgeEntryCreateRequest request = new KnowledgeEntryCreateRequest(
                "DRAWING", "DWG-HVAC-001", "任意标题", null, "V1.0",
                null, null, null, null, null, null);

        assertEquals(1, knowledgeEntryMapper.countActiveByBusinessKey(request, null));
    }

    /** 修改 SQL 应更新全部业务元数据、标记为已完善，并保留原有当前文件。 */
    @Test
    void shouldUpdateKnowledgeEntry() {
        long entryId = 2100000000000000001L;
        KnowledgeEntryUpdateRequest request = new KnowledgeEntryUpdateRequest(
                "DRAWING", "DWG-HVAC-001", "修改后的暖通图纸", "修改 暖通", "V1.1",
                "修改项目", LocalDate.of(2026, 7, 1), "新图纸库", "HVAC", "新编写人", "机密");

        assertEquals(1, knowledgeEntryMapper.countActiveById(entryId));
        assertEquals(0, knowledgeEntryMapper.countActiveByBusinessKey(request, entryId));
        assertEquals(1, knowledgeEntryMapper.updateEntry(entryId, request));

        KnowledgeEntryDetailRow row = knowledgeEntryMapper.selectDetail(entryId);
        assertEquals("修改后的暖通图纸", row.title());
        assertEquals("V1.1", row.version());
        assertEquals("修改项目", row.projectName());
        assertEquals(1, row.infoStatus());
        assertEquals("2200000000000000001", row.fileId());
    }

    /** 更新条件必须阻止已逻辑删除条目被重新激活或修改。 */
    @Test
    void shouldNotUpdateLogicallyDeletedEntry() {
        long deletedEntryId = 2100000000000000003L;
        KnowledgeEntryUpdateRequest request = new KnowledgeEntryUpdateRequest(
                "LAW", "LAW-001", "尝试修改删除记录", null, "V2.0",
                null, null, null, null, null, null);

        assertEquals(0, knowledgeEntryMapper.countActiveById(deletedEntryId));
        assertEquals(0, knowledgeEntryMapper.updateEntry(deletedEntryId, request));
    }
}
