package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.api.PageResponse;
import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.common.id.LongIdGenerator;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryCreateRequest;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryBatchDeleteRequest;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryPageRequest;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryUpdateRequest;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryCreateResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryBatchDeleteResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryDetailResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryPageItemResponse;
import com.example.dqcadirsystem.knowledge.mapper.KnowledgeEntryMapper;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeEntryDetailRow;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeEntryPageRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 知识条目分页业务逻辑单元测试。
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeEntryServiceTest {

    @Mock
    private KnowledgeEntryMapper knowledgeEntryMapper;

    @Mock
    private LongIdGenerator longIdGenerator;

    @InjectMocks
    private KnowledgeEntryService knowledgeEntryService;

    /** 验证总页数计算、枚举名称转换和默认分页参数。 */
    @Test
    void shouldBuildPageResponse() {
        KnowledgeEntryPageRequest request = request(" DRAWING ", null, null, null);
        KnowledgeEntryPageRow row = new KnowledgeEntryPageRow(
                "2100000000000000001", "2200000000000000001", "DRAWING", "DWG-HVAC-001",
                "暖通图纸", "内部", "V1.0", LocalDate.of(2026, 6, 26), "pdf", "HVAC", "张工", 1,
                "暖通图纸.pdf", "/uploads/knowledge/1.pdf",
                LocalDateTime.of(2026, 6, 30, 10, 0), LocalDateTime.of(2026, 6, 30, 10, 0));
        when(knowledgeEntryMapper.countPage(request)).thenReturn(11L);
        when(knowledgeEntryMapper.selectPage(request)).thenReturn(List.of(row));

        PageResponse<KnowledgeEntryPageItemResponse> result = knowledgeEntryService.pageEntries(request);

        assertEquals(11, result.total());
        assertEquals(1, result.pageNum());
        assertEquals(10, result.pageSize());
        assertEquals(2, result.pages());
        assertEquals("DRAWING", request.entryType());
        assertEquals("图纸库", result.records().getFirst().entryTypeName());
        assertEquals("已完善", result.records().getFirst().infoStatusName());
    }

    /** 总数为零时不执行列表查询。 */
    @Test
    void shouldSkipPageQueryWhenNoRecordsMatch() {
        KnowledgeEntryPageRequest request = request(null, null, null, null);
        when(knowledgeEntryMapper.countPage(request)).thenReturn(0L);

        PageResponse<KnowledgeEntryPageItemResponse> result = knowledgeEntryService.pageEntries(request);

        assertEquals(0, result.pages());
        assertEquals(List.of(), result.records());
        verify(knowledgeEntryMapper, never()).selectPage(request);
    }

    /** 不支持的知识条目类型应作为请求参数错误处理。 */
    @Test
    void shouldRejectUnknownEntryType() {
        KnowledgeEntryPageRequest request = request("UNKNOWN", null, null, null);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> knowledgeEntryService.pageEntries(request));

        assertEquals("知识条目类型不合法", exception.getMessage());
        verify(knowledgeEntryMapper, never()).countPage(request);
    }

    /** 发版开始日期不能晚于结束日期。 */
    @Test
    void shouldRejectReversedReleaseDateRange() {
        KnowledgeEntryPageRequest request = request(
                null, LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 1), null);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> knowledgeEntryService.pageEntries(request));

        assertEquals("发版开始日期不能晚于结束日期", exception.getMessage());
    }

    /** 验证详情查询会补充枚举中文名称，并把平铺的文件字段组装为 currentFile。 */
    @Test
    void shouldBuildEntryDetailResponse() {
        KnowledgeEntryDetailRow row = detailRow("2200000000000000001");
        when(knowledgeEntryMapper.selectDetail(2100000000000000001L)).thenReturn(row);

        KnowledgeEntryDetailResponse result = knowledgeEntryService.getEntryDetail(2100000000000000001L);

        assertEquals("2100000000000000001", result.entryId());
        assertEquals("图纸库", result.entryTypeName());
        assertEquals("已完善", result.infoStatusName());
        assertEquals("2200000000000000001", result.currentFile().fileId());
        assertEquals(2048000L, result.currentFile().fileSize());
    }

    /** 合法条目没有当前文件时，响应中的 currentFile 应为 null。 */
    @Test
    void shouldReturnNullCurrentFileWhenEntryHasNoFile() {
        KnowledgeEntryDetailRow row = detailRow(null);
        when(knowledgeEntryMapper.selectDetail(2100000000000000001L)).thenReturn(row);

        KnowledgeEntryDetailResponse result = knowledgeEntryService.getEntryDetail(2100000000000000001L);

        assertEquals(null, result.currentFile());
    }

    /** 不存在和已逻辑删除的条目都应使用同一个 404 业务语义。 */
    @Test
    void shouldThrowNotFoundWhenEntryDoesNotExist() {
        when(knowledgeEntryMapper.selectDetail(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> knowledgeEntryService.getEntryDetail(999L));

        assertEquals(CommonErrorCode.NOT_FOUND, exception.getErrorCode());
        assertEquals("知识条目不存在", exception.getMessage());
    }

    /** 正常新增时应生成主键、插入一次，并以字符串返回该主键。 */
    @Test
    void shouldCreateKnowledgeEntry() {
        KnowledgeEntryCreateRequest request = createRequest("DRAWING", "DWG-NEW-001", "V1.0");
        when(knowledgeEntryMapper.countActiveByBusinessKey(request, null)).thenReturn(0);
        when(longIdGenerator.nextId()).thenReturn(2100000000000000099L);
        when(knowledgeEntryMapper.insertEntry(2100000000000000099L, request)).thenReturn(1);

        KnowledgeEntryCreateResponse result = knowledgeEntryService.createEntry(request);

        assertEquals("2100000000000000099", result.entryId());
        verify(knowledgeEntryMapper).insertEntry(2100000000000000099L, request);
    }

    /** 不合法类型应在查询数据库和生成 ID 之前被拒绝。 */
    @Test
    void shouldRejectUnknownEntryTypeWhenCreating() {
        KnowledgeEntryCreateRequest request = createRequest("UNKNOWN", "CODE-001", "V1.0");

        BusinessException exception = assertThrows(
                BusinessException.class, () -> knowledgeEntryService.createEntry(request));

        assertEquals(CommonErrorCode.BAD_REQUEST, exception.getErrorCode());
        assertEquals("知识条目类型不合法", exception.getMessage());
        verify(knowledgeEntryMapper, never()).countActiveByBusinessKey(request, null);
        verify(longIdGenerator, never()).nextId();
    }

    /** 已存在相同正常业务键时，不应生成 ID 或再次插入。 */
    @Test
    void shouldRejectDuplicateBusinessKeyBeforeInsert() {
        KnowledgeEntryCreateRequest request = createRequest("DRAWING", "DWG-HVAC-001", "V1.0");
        when(knowledgeEntryMapper.countActiveByBusinessKey(request, null)).thenReturn(1);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> knowledgeEntryService.createEntry(request));

        assertEquals(CommonErrorCode.BUSINESS_ERROR, exception.getErrorCode());
        assertEquals("相同类型、条目编码和版本的知识条目已存在", exception.getMessage());
        verify(longIdGenerator, never()).nextId();
        verify(knowledgeEntryMapper, never()).insertEntry(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
    }

    /** 并发请求绕过前置检查时，数据库唯一键异常仍应转换为稳定的业务错误。 */
    @Test
    void shouldTranslateConcurrentDuplicateKeyToBusinessException() {
        KnowledgeEntryCreateRequest request = createRequest("DRAWING", "DWG-NEW-001", "V1.0");
        // 第一次检查尚不存在；模拟另一事务抢先插入后，异常兜底检查能够看到重复记录。
        when(knowledgeEntryMapper.countActiveByBusinessKey(request, null)).thenReturn(0, 1);
        when(longIdGenerator.nextId()).thenReturn(2100000000000000099L);
        when(knowledgeEntryMapper.insertEntry(2100000000000000099L, request))
                .thenThrow(new DuplicateKeyException("duplicate business key"));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> knowledgeEntryService.createEntry(request));

        assertEquals(CommonErrorCode.BUSINESS_ERROR, exception.getErrorCode());
        assertEquals("相同类型、条目编码和版本的知识条目已存在", exception.getMessage());
    }

    /** 正常修改应排除当前条目进行重复检查，并返回 true。 */
    @Test
    void shouldUpdateKnowledgeEntry() {
        KnowledgeEntryUpdateRequest request = updateRequest("DRAWING", "DWG-HVAC-001", "V1.1");
        when(knowledgeEntryMapper.countActiveById(2100000000000000001L)).thenReturn(1);
        when(knowledgeEntryMapper.countActiveByBusinessKey(request, 2100000000000000001L)).thenReturn(0);
        when(knowledgeEntryMapper.updateEntry(2100000000000000001L, request)).thenReturn(1);

        boolean result = knowledgeEntryService.updateEntry(2100000000000000001L, request);

        assertEquals(true, result);
        verify(knowledgeEntryMapper).updateEntry(2100000000000000001L, request);
    }

    /** 不存在或已逻辑删除的条目不允许修改。 */
    @Test
    void shouldRejectUpdateWhenEntryDoesNotExist() {
        KnowledgeEntryUpdateRequest request = updateRequest("DRAWING", "DWG-HVAC-001", "V1.1");
        when(knowledgeEntryMapper.countActiveById(999L)).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> knowledgeEntryService.updateEntry(999L, request));

        assertEquals(CommonErrorCode.NOT_FOUND, exception.getErrorCode());
        assertEquals("知识条目不存在", exception.getMessage());
        verify(knowledgeEntryMapper, never()).updateEntry(999L, request);
    }

    /** 修改后的业务键不得与另一条正常记录重复。 */
    @Test
    void shouldRejectDuplicateBusinessKeyWhenUpdating() {
        KnowledgeEntryUpdateRequest request = updateRequest("DRAWING", "DWG-CIVIL-002", "V1.0");
        when(knowledgeEntryMapper.countActiveById(2100000000000000001L)).thenReturn(1);
        when(knowledgeEntryMapper.countActiveByBusinessKey(request, 2100000000000000001L)).thenReturn(1);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> knowledgeEntryService.updateEntry(2100000000000000001L, request));

        assertEquals(CommonErrorCode.BUSINESS_ERROR, exception.getErrorCode());
        verify(knowledgeEntryMapper, never()).updateEntry(2100000000000000001L, request);
    }

    /** 并发占用业务键导致数据库冲突时，应转换为与前置检查一致的业务异常。 */
    @Test
    void shouldTranslateConcurrentDuplicateWhenUpdating() {
        KnowledgeEntryUpdateRequest request = updateRequest("DRAWING", "DWG-NEW-001", "V1.0");
        when(knowledgeEntryMapper.countActiveById(2100000000000000001L)).thenReturn(1);
        when(knowledgeEntryMapper.countActiveByBusinessKey(request, 2100000000000000001L))
                .thenReturn(0, 1);
        when(knowledgeEntryMapper.updateEntry(2100000000000000001L, request))
                .thenThrow(new DuplicateKeyException("duplicate business key"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> knowledgeEntryService.updateEntry(2100000000000000001L, request));

        assertEquals(CommonErrorCode.BUSINESS_ERROR, exception.getErrorCode());
        assertEquals("相同类型、条目编码和版本的知识条目已存在", exception.getMessage());
    }

    /** 单条删除成功后必须继续失效该条目的全部正常文件。 */
    @Test
    void shouldDeleteEntryAndItsFiles() {
        long entryId = 2100000000000000001L;
        when(knowledgeEntryMapper.logicalDeleteEntry(entryId)).thenReturn(1);
        when(knowledgeEntryMapper.logicalDeleteFilesByEntryId(entryId)).thenReturn(2);

        boolean result = knowledgeEntryService.deleteEntry(entryId);

        assertEquals(true, result);
        verify(knowledgeEntryMapper).logicalDeleteFilesByEntryId(entryId);
    }

    /** 单条删除目标不存在时返回 404，且不应执行文件更新。 */
    @Test
    void shouldRejectDeleteWhenEntryDoesNotExist() {
        when(knowledgeEntryMapper.logicalDeleteEntry(999L)).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> knowledgeEntryService.deleteEntry(999L));

        assertEquals(CommonErrorCode.NOT_FOUND, exception.getErrorCode());
        assertEquals("知识条目不存在", exception.getMessage());
        verify(knowledgeEntryMapper, never()).logicalDeleteFilesByEntryId(999L);
    }

    /** 批量删除对不存在条目记录失败明细，同时继续处理其他正常条目。 */
    @Test
    void shouldReturnPartialResultWhenBatchDeleteContainsMissingEntry() {
        KnowledgeEntryBatchDeleteRequest request = new KnowledgeEntryBatchDeleteRequest(
                List.of(2100000000000000001L, 999L, 2100000000000000002L));
        when(knowledgeEntryMapper.logicalDeleteEntry(2100000000000000001L)).thenReturn(1);
        when(knowledgeEntryMapper.logicalDeleteEntry(999L)).thenReturn(0);
        when(knowledgeEntryMapper.logicalDeleteEntry(2100000000000000002L)).thenReturn(1);

        KnowledgeEntryBatchDeleteResponse result = knowledgeEntryService.batchDeleteEntries(request);

        assertEquals(2, result.successCount());
        assertEquals(1, result.failedCount());
        assertEquals("999", result.failedList().getFirst().entryId());
        assertEquals("知识条目不存在", result.failedList().getFirst().reason());
        verify(knowledgeEntryMapper).logicalDeleteFilesByEntryId(2100000000000000001L);
        verify(knowledgeEntryMapper).logicalDeleteFilesByEntryId(2100000000000000002L);
        verify(knowledgeEntryMapper, never()).logicalDeleteFilesByEntryId(999L);
    }

    /** 重复 ID 会造成成功和失败数量歧义，因此整批请求应在写数据库前被拒绝。 */
    @Test
    void shouldRejectDuplicateIdsInBatchDelete() {
        KnowledgeEntryBatchDeleteRequest request = new KnowledgeEntryBatchDeleteRequest(List.of(1L, 1L));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> knowledgeEntryService.batchDeleteEntries(request));

        assertEquals(CommonErrorCode.BAD_REQUEST, exception.getErrorCode());
        assertEquals("知识条目ID不能重复", exception.getMessage());
        verify(knowledgeEntryMapper, never()).logicalDeleteEntry(org.mockito.ArgumentMatchers.anyLong());
    }

    private KnowledgeEntryPageRequest request(
            String entryType, LocalDate startDate, LocalDate endDate, Integer pageNum) {
        return new KnowledgeEntryPageRequest(
                entryType, null, null, null, null, null, startDate, endDate, pageNum, null);
    }

    /** 创建只包含新增场景必要业务数据的请求对象。 */
    private KnowledgeEntryCreateRequest createRequest(String entryType, String entryCode, String version) {
        return new KnowledgeEntryCreateRequest(
                entryType, entryCode, "新增知识条目", "新增 知识", version,
                null, LocalDate.of(2026, 6, 30), null, null, null, null);
    }

    /** 创建一条字段完整的修改请求。 */
    private KnowledgeEntryUpdateRequest updateRequest(String entryType, String entryCode, String version) {
        return new KnowledgeEntryUpdateRequest(
                entryType, entryCode, "修改后的知识条目", "修改 知识", version,
                "修改项目", LocalDate.of(2026, 7, 1), "知识库", "HVAC", "修改人", "内部");
    }

    /** 创建一条详情测试投影；fileId 为空时同步清空全部文件字段。 */
    private KnowledgeEntryDetailRow detailRow(String fileId) {
        boolean hasFile = fileId != null;
        return new KnowledgeEntryDetailRow(
                "2100000000000000001", "DRAWING", "DWG-HVAC-001", "总部办公楼暖通空调设计图1",
                "总部 暖通 空调", "V1.0", "总部项目", LocalDate.of(2026, 6, 26), "图纸库",
                "HVAC", "张工", "内部", 1, fileId,
                hasFile ? "总部办公楼暖通空调设计图1.pdf" : null,
                hasFile ? "pdf" : null,
                hasFile ? 2048000L : null,
                hasFile ? "/uploads/knowledge/2100000000000000001.pdf" : null,
                hasFile ? "success" : null,
                hasFile ? LocalDateTime.of(2026, 6, 30, 10, 0) : null);
    }
}
