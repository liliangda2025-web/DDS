package com.example.dqcadirsystem.knowledge.controller;

import com.example.dqcadirsystem.common.api.PageResponse;
import com.example.dqcadirsystem.common.exception.BusinessException;
import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.common.exception.GlobalExceptionHandler;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryCreateRequest;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryBatchDeleteRequest;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryPageRequest;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryUpdateRequest;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeCurrentFileResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryBatchDeleteResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryDeleteFailureResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryDetailResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryCreateResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryPageItemResponse;
import com.example.dqcadirsystem.knowledge.service.KnowledgeEntryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 知识条目查询接口的 Web 层契约测试。
 */
@WebMvcTest(KnowledgeEntryController.class)
@Import(GlobalExceptionHandler.class)
class KnowledgeEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** Service 在 Web 层测试中使用模拟对象，Controller 测试不连接数据库。 */
    @MockitoBean
    private KnowledgeEntryService knowledgeEntryService;

    /**
     * 验证外部完整路径、统一响应、分页元数据、字符串 ID 和时间格式。
     */
    @Test
    void shouldReturnKnowledgeEntryPage() throws Exception {
        KnowledgeEntryPageItemResponse item = new KnowledgeEntryPageItemResponse(
                "2100000000000000001",
                "2200000000000000001",
                "DRAWING",
                "图纸库",
                "DWG-HVAC-001",
                "总部办公楼暖通空调设计图1",
                "内部",
                "V1.0",
                LocalDate.of(2026, 6, 26),
                "pdf",
                "HVAC",
                "张工",
                1,
                "已完善",
                "总部办公楼暖通空调设计图1.pdf",
                LocalDateTime.of(2026, 6, 30, 10, 0),
                LocalDateTime.of(2026, 6, 30, 10, 0));
        when(knowledgeEntryService.pageEntries(any(KnowledgeEntryPageRequest.class)))
                .thenReturn(PageResponse.of(1, 1, 10, List.of(item)));

        mockMvc.perform(post("/api/knowledge/entries/page")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.pages").value(1))
                .andExpect(jsonPath("$.data.records[0].entryId").value("2100000000000000001"))
                .andExpect(jsonPath("$.data.records[0].entryTypeName").value("图纸库"))
                .andExpect(jsonPath("$.data.records[0].fileUrl").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].createdAt").value("2026-06-30 10:00:00"));
    }

    /** 验证页码不合法时由统一异常处理器返回参数错误。 */
    @Test
    void shouldRejectInvalidPageNumber() throws Exception {
        mockMvc.perform(post("/api/knowledge/entries/page")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageNum\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("页码必须大于等于1"));
    }

    /** 防止单次查询返回过多数据，每页条数上限固定为 100。 */
    @Test
    void shouldRejectOversizedPage() throws Exception {
        mockMvc.perform(post("/api/knowledge/entries/page")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageSize\":101}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("每页条数不能超过100"));
    }

    /** 验证详情接口字段、嵌套当前文件、字符串 ID 和时间格式与接口文档一致。 */
    @Test
    void shouldReturnKnowledgeEntryDetail() throws Exception {
        KnowledgeCurrentFileResponse currentFile = new KnowledgeCurrentFileResponse(
                "2200000000000000001",
                "总部办公楼暖通空调设计图1.pdf",
                "pdf",
                2048000L,
                "success",
                LocalDateTime.of(2026, 6, 30, 10, 0));
        KnowledgeEntryDetailResponse detail = new KnowledgeEntryDetailResponse(
                "2100000000000000001", "DRAWING", "图纸库", "DWG-HVAC-001",
                "总部办公楼暖通空调设计图1", "总部 暖通 空调", "V1.0", "总部项目",
                LocalDate.of(2026, 6, 26), "图纸库", "HVAC", "张工", "内部",
                1, "已完善", currentFile);
        when(knowledgeEntryService.getEntryDetail(2100000000000000001L)).thenReturn(detail);

        mockMvc.perform(get("/api/knowledge/entries/2100000000000000001").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.entryId").value("2100000000000000001"))
                .andExpect(jsonPath("$.data.entryTypeName").value("图纸库"))
                .andExpect(jsonPath("$.data.infoStatusName").value("已完善"))
                .andExpect(jsonPath("$.data.currentFile.fileId").value("2200000000000000001"))
                .andExpect(jsonPath("$.data.currentFile.fileUrl").doesNotExist())
                .andExpect(jsonPath("$.data.currentFile.fileSize").value(2048000))
                .andExpect(jsonPath("$.data.currentFile.uploadedAt").value("2026-06-30 10:00:00"));
    }

    /** Service 判定资源不存在时，统一异常处理器应返回 HTTP 404 和业务码 40400。 */
    @Test
    void shouldReturnNotFoundWhenEntryDoesNotExist() throws Exception {
        when(knowledgeEntryService.getEntryDetail(999L))
                .thenThrow(new BusinessException(CommonErrorCode.NOT_FOUND, "知识条目不存在"));

        mockMvc.perform(get("/api/knowledge/entries/999").contextPath("/api"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value("知识条目不存在"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /** 非正数 ID 不应进入 Service，而应直接返回统一参数错误。 */
    @Test
    void shouldRejectNonPositiveEntryId() throws Exception {
        mockMvc.perform(get("/api/knowledge/entries/0").contextPath("/api"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("知识条目ID必须大于0"));
    }

    /** 验证新增接口使用自定义成功提示，并以字符串形式返回新条目 ID。 */
    @Test
    void shouldCreateKnowledgeEntry() throws Exception {
        when(knowledgeEntryService.createEntry(any(KnowledgeEntryCreateRequest.class)))
                .thenReturn(new KnowledgeEntryCreateResponse("2100000000000000099"));

        mockMvc.perform(post("/api/knowledge/entries")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entryType": "DRAWING",
                                  "entryCode": "DWG-NEW-001",
                                  "title": "新增图纸",
                                  "keywords": "新增 图纸",
                                  "version": "V1.0",
                                  "releaseDate": "2026-06-30"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("新增成功"))
                .andExpect(jsonPath("$.data.entryId").value("2100000000000000099"));
    }

    /** 缺少关键业务字段时应在 Controller 层直接返回统一参数错误。 */
    @Test
    void shouldRejectCreateRequestWithoutTitle() throws Exception {
        mockMvc.perform(post("/api/knowledge/entries")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entryType": "DRAWING",
                                  "entryCode": "DWG-NEW-001",
                                  "version": "V1.0"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("标题不能为空"));
    }

    /** 验证修改接口的路径、请求体和成功响应结构。 */
    @Test
    void shouldUpdateKnowledgeEntry() throws Exception {
        when(knowledgeEntryService.updateEntry(
                org.mockito.ArgumentMatchers.eq(2100000000000000001L),
                any(KnowledgeEntryUpdateRequest.class))).thenReturn(true);

        mockMvc.perform(put("/api/knowledge/entries/2100000000000000001")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entryType": "DRAWING",
                                  "entryCode": "DWG-HVAC-001",
                                  "title": "修改后的暖通图纸",
                                  "version": "V1.1",
                                  "releaseDate": "2026-07-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("修改成功"))
                .andExpect(jsonPath("$.data").value(true));
    }

    /** 修改接口的路径 ID 必须是正整数。 */
    @Test
    void shouldRejectUpdateWithNonPositiveEntryId() throws Exception {
        mockMvc.perform(put("/api/knowledge/entries/0")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entryType": "DRAWING",
                                  "entryCode": "DWG-HVAC-001",
                                  "title": "修改后的暖通图纸",
                                  "version": "V1.1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("知识条目ID必须大于0"));
    }

    /** 验证单条删除返回文档约定的布尔结果。 */
    @Test
    void shouldDeleteKnowledgeEntry() throws Exception {
        when(knowledgeEntryService.deleteEntry(2100000000000000001L)).thenReturn(true);

        mockMvc.perform(delete("/api/knowledge/entries/2100000000000000001").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"))
                .andExpect(jsonPath("$.data").value(true));
    }

    /** 验证批量删除可以同时返回成功数量和按输入 ID 标识的失败明细。 */
    @Test
    void shouldBatchDeleteKnowledgeEntries() throws Exception {
        KnowledgeEntryBatchDeleteResponse response = KnowledgeEntryBatchDeleteResponse.of(
                1, List.of(KnowledgeEntryDeleteFailureResponse.notFound(999L)));
        when(knowledgeEntryService.batchDeleteEntries(any(KnowledgeEntryBatchDeleteRequest.class)))
                .thenReturn(response);

        mockMvc.perform(delete("/api/knowledge/entries/batch")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entryIds": ["2100000000000000001", "999"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.failedList[0].entryId").value("999"))
                .andExpect(jsonPath("$.data.failedList[0].reason").value("知识条目不存在"));
    }

    /** 批量删除至少需要提供一个知识条目 ID。 */
    @Test
    void shouldRejectEmptyBatchDeleteRequest() throws Exception {
        mockMvc.perform(delete("/api/knowledge/entries/batch")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entryIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("知识条目ID列表不能为空"));
    }

    /** 防止逐条删除产生过多数据库操作，单批最多允许 100 个 ID。 */
    @Test
    void shouldRejectOversizedBatchDeleteRequest() throws Exception {
        String entryIds = java.util.stream.LongStream.rangeClosed(1, 101)
                .mapToObj(entryId -> "\"" + entryId + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));

        mockMvc.perform(delete("/api/knowledge/entries/batch")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entryIds\":" + entryIds + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("单次最多删除100条知识条目"));
    }
}
