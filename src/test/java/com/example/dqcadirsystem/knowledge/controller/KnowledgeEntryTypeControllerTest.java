package com.example.dqcadirsystem.knowledge.controller;

import com.example.dqcadirsystem.common.exception.GlobalExceptionHandler;
import com.example.dqcadirsystem.knowledge.service.KnowledgeEntryTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 获取知识条目类型接口的 Web 层测试。
 *
 * <p>{@link WebMvcTest} 只加载 Controller 及 Spring MVC 组件；这里显式导入真实服务，而不是模拟返回值，
 * 因此一次请求可以同时验证枚举、DTO 转换、服务层、统一响应和 JSON 序列化。数据库相关组件不会启动，
 * 也就证明该接口不依赖数据库。</p>
 */
@WebMvcTest(KnowledgeEntryTypeController.class)
@Import({KnowledgeEntryTypeService.class, GlobalExceptionHandler.class})
class KnowledgeEntryTypeControllerTest {

    /** 在不启动真实 HTTP 端口的情况下模拟浏览器请求。 */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证无需请求参数即可获得四种类型，并严格校验业务码、提示、顺序、编码和中文名称。
     */
    @Test
    void shouldReturnAllKnowledgeEntryTypesInContractOrder() throws Exception {
        mockMvc.perform(get("/api/knowledge/entry-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].label").value("图纸库"))
                .andExpect(jsonPath("$.data[0].value").value("DRAWING"))
                .andExpect(jsonPath("$.data[1].label").value("程序生效准则"))
                .andExpect(jsonPath("$.data[1].value").value("PROGRAM_RULE"))
                .andExpect(jsonPath("$.data[2].label").value("法律法规"))
                .andExpect(jsonPath("$.data[2].value").value("LAW"))
                .andExpect(jsonPath("$.data[3].label").value("历史案例"))
                .andExpect(jsonPath("$.data[3].value").value("CASE"));
    }
}
