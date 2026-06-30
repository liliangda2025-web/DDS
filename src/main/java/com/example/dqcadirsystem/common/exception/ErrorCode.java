package com.example.dqcadirsystem.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 错误码统一契约。
 *
 * <p>公共错误由 {@link CommonErrorCode} 提供。以后用户、设备、任务等业务模块可以分别定义自己的枚举，
 * 只要实现本接口，就能直接交给 {@link BusinessException} 和全局异常处理器使用，避免所有错误码都堆积
 * 在一个巨大枚举中。</p>
 *
 * <p>一个错误同时包含“业务码”和“HTTP 状态码”：业务码供前端进行精细化判断，HTTP 状态码供浏览器、
 * 网关和监控系统识别请求结果。</p>
 */
public interface ErrorCode {

    /**
     * 返回应用内部业务码，例如参数错误为 40000。
     */
    int code();

    /**
     * 返回该错误的默认安全提示，不应包含 SQL、堆栈或服务器内部信息。
     */
    String message();

    /**
     * 返回该错误对应的语义化 HTTP 状态码。
     */
    HttpStatus status();
}
