package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.common.api.PageResponse;
import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryPageRequest;
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

    private KnowledgeEntryPageRequest request(
            String entryType, LocalDate startDate, LocalDate endDate, Integer pageNum) {
        return new KnowledgeEntryPageRequest(
                entryType, null, null, null, null, null, startDate, endDate, pageNum, null);
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
