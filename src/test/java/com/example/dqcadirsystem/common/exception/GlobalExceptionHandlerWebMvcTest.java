package com.example.dqcadirsystem.common.exception;

import com.example.dqcadirsystem.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全局响应与异常处理的 Web 层集成测试。
 *
 * <p>{@link WebMvcTest} 只启动 Spring MVC 相关组件，不加载数据库、MyBatis 等无关基础设施，
 * 因此测试速度更快，也能准确验证 Controller 到异常处理器再到 JSON 响应的完整链路。</p>
 *
 * <p>下方 {@link TestController} 只存在于测试环境，用于主动制造各种成功和失败场景，
 * 不会成为生产接口。</p>
 */
@WebMvcTest(controllers = GlobalExceptionHandlerWebMvcTest.TestController.class)
// 显式导入异常处理器和测试 Controller，确保 Web 测试切片只包含本测试所需组件。
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerWebMvcTest.TestController.class})
class GlobalExceptionHandlerWebMvcTest {

    /**
     * MockMvc 可以在不启动真实 HTTP 端口的情况下模拟请求，并断言状态码与 JSON 内容。
     */
    @Autowired
    private MockMvc mockMvc;

    /** 验证携带业务数据的成功响应固定包含 code、message、data。 */
    @Test
    void shouldReturnWrappedSuccessResponse() throws Exception {
        mockMvc.perform(get("/test/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").value("result"));
    }

    /** 验证没有业务数据时 data 字段仍然存在，并明确序列化为 null。 */
    @Test
    void shouldKeepNullDataInEmptySuccessResponse() throws Exception {
        mockMvc.perform(get("/test/empty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    /** 验证业务异常会保留安全提示，并同时返回业务码 40001 和 HTTP 400。 */
    @Test
    void shouldHandleBusinessException() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("库存不足"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    /** 验证 @Valid 请求体校验失败时只返回第一条字段校验信息。 */
    @Test
    void shouldReturnFirstBeanValidationMessage() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("名称不能为空"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    /** 验证缺少必填查询参数时能够指出具体参数名。 */
    @Test
    void shouldHandleMissingRequestParameter() throws Exception {
        mockMvc.perform(get("/test/required"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("缺少必填参数: name"));
    }

    /** 验证查询参数无法转换成 Controller 声明的 Java 类型时返回参数错误。 */
    @Test
    void shouldHandleRequestParameterTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/number").param("count", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("参数类型错误: count"));
    }

    /** 验证无法解析的 JSON 不会暴露 Jackson 的内部错误详情。 */
    @Test
    void shouldHandleMalformedJson() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        // 只发送左花括号，确保请求体是语法不完整的 JSON，而不是可正常解析的空对象。
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("请求体格式错误"));
    }

    /** 验证没有任何 Controller 匹配的路径被统一转换为 40400。 */
    @Test
    void shouldHandleMissingResource() throws Exception {
        mockMvc.perform(get("/missing-resource"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value("资源不存在"));
    }

    /** 验证路径存在但 HTTP 方法不匹配时返回 HTTP 405 和业务码 40500。 */
    @Test
    void shouldHandleUnsupportedMethod() throws Exception {
        mockMvc.perform(post("/test/success"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(40500))
                .andExpect(jsonPath("$.message").value("请求方法不支持"));
    }

    /** 上传体积超过限制时，也必须保持统一参数错误响应。 */
    @Test
    void shouldHandleMaxUploadSizeExceeded() throws Exception {
        mockMvc.perform(get("/test/file-too-large"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("文件大小不能超过500MB"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    /**
     * 验证未知系统异常只向客户端返回固定安全提示。
     * 测试 Controller 抛出的敏感异常文本只能出现在服务端日志中，不能出现在 JSON 响应中。
     */
    @Test
    void shouldHideUnexpectedExceptionDetails() throws Exception {
        mockMvc.perform(get("/test/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(50000))
                .andExpect(jsonPath("$.message").value("系统繁忙，请稍后重试"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    /**
     * 专供本测试使用的 Controller，通过不同端点构造统一响应和各类异常场景。
     */
    @RestController
    @RequestMapping("/test")
    static class TestController {

        /** 返回一个最常见的“成功且有数据”响应。 */
        @GetMapping("/success")
        ApiResponse<String> success() {
            return ApiResponse.success("result");
        }

        /** 返回一个“成功但无数据”响应。 */
        @GetMapping("/empty")
        ApiResponse<Void> empty() {
            return ApiResponse.success();
        }

        /** 模拟业务层发现库存不足并主动中断流程。 */
        @GetMapping("/business")
        ApiResponse<Void> business() {
            throw new BusinessException("库存不足");
        }

        /**  会在进入方法体之前校验 CreateRequest。 */
        @Valid
        @PostMapping("/validate")
        ApiResponse<String> validate(@Valid @RequestBody CreateRequest request) {
            return ApiResponse.success(request.name());
        }

        /** 未声明 required=false，因此 name 默认是必填查询参数。 */
        @GetMapping("/required")
        ApiResponse<String> required(@RequestParam String name) {
            return ApiResponse.success(name);
        }

        /** Spring MVC 会尝试把请求字符串转换为 Integer，转换失败时抛出类型不匹配异常。 */
        @GetMapping("/number")
        ApiResponse<Integer> number(@RequestParam Integer count) {
            return ApiResponse.success(count);
        }

        /** 模拟 multipart 解析阶段发现文件超过配置限制。 */
        @GetMapping("/file-too-large")
        ApiResponse<Void> fileTooLarge() {
            throw new MaxUploadSizeExceededException(500L * 1024 * 1024);
        }

        /** 模拟未预料的系统异常，用于验证兜底处理和敏感信息保护。 */
        @GetMapping("/error")
        ApiResponse<Void> error() {
            throw new IllegalStateException("sensitive implementation detail");
        }
    }

    /**
     * 测试用请求 DTO：name 为空时，校验器会产生明确的中文提示。
     */
    record CreateRequest(@NotBlank(message = "名称不能为空") String name) {
    }
}
