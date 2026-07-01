package com.example.dqcadirsystem.knowledge.controller;

import com.example.dqcadirsystem.common.exception.GlobalExceptionHandler;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeSearchItemResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeSearchResponse;
import com.example.dqcadirsystem.knowledge.service.KnowledgeSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 知识检索HTTP路径、默认校验和响应字段边界测试。 */
@WebMvcTest(KnowledgeSearchController.class)
@Import(GlobalExceptionHandler.class)
class KnowledgeSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KnowledgeSearchService searchService;

    @Test
    void shouldReturnSearchResponseWithoutUrlOrScore() throws Exception {
        KnowledgeSearchItemResponse item = new KnowledgeSearchItemResponse(
                "1", "2", "DRAWING", "图纸库", "DWG-001", "暖通图纸", "暖通图纸",
                "暖通 空调", "项目", "图纸库", "HVAC", "张工", "暖通.pdf", "pdf");
        when(searchService.search(any())).thenReturn(new KnowledgeSearchResponse(
                1, 1, 10, 1, new BigDecimal("0.12"),
                "共找到1份相关图纸，耗时0.12s", List.of(item)));

        mockMvc.perform(post("/api/knowledge/search")
                        .contextPath("/api")
                        .contentType("application/json")
                        .content("""
                                {"queryText":"暖通","entryType":"DRAWING"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("检索成功"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.costSeconds").value(0.12))
                .andExpect(jsonPath("$.data.records[0].entryTypeName").value("图纸库"))
                .andExpect(jsonPath("$.data.records[0].fileUrl").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].score").doesNotExist());
    }

    @Test
    void shouldRejectBlankQueryText() throws Exception {
        mockMvc.perform(post("/api/knowledge/search")
                        .contextPath("/api")
                        .contentType("application/json")
                        .content("{\"queryText\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("查询内容不能为空"));
    }

    @Test
    void shouldRejectPageSizeOverOneHundred() throws Exception {
        mockMvc.perform(post("/api/knowledge/search")
                        .contextPath("/api")
                        .contentType("application/json")
                        .content("{\"queryText\":\"暖通\",\"pageSize\":101}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("每页条数不能超过100"));
    }
}
