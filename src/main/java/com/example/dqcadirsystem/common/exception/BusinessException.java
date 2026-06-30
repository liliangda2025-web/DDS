package com.example.dqcadirsystem.common.exception;

import java.util.Objects;

/**
 * 表示“可预期、可安全告知调用方”的业务异常。
 *
 * <p>例如库存不足、记录状态不允许修改、设备已经绑定等情况都属于业务异常。业务层只需要抛出本异常，
 * 不需要在每个 Controller 中重复编写 try/catch；{@link GlobalExceptionHandler} 会统一转换为 HTTP 响应。</p>
 *
 * <p>不要使用本异常包装数据库连接失败、空指针等系统故障，否则可能把内部信息暴露给调用方。
 * 系统故障应保持原异常，由兜底异常处理器记录完整堆栈并返回固定提示。</p>
 */
public class BusinessException extends RuntimeException {

    /** 决定响应业务码、默认提示以及 HTTP 状态码。 */
    private final ErrorCode errorCode;

    /**
     * 使用通用业务错误码和自定义提示。
     *
     * <p>示例：{@code throw new BusinessException("库存不足");}</p>
     */
    public BusinessException(String message) {
        this(CommonErrorCode.BUSINESS_ERROR, message);
    }

    /**
     * 使用指定错误码及其默认提示。
     *
     * <p>适合业务模块已经为该错误定义了明确错误码的场景。</p>
     */
    public BusinessException(ErrorCode errorCode) {
        this(errorCode, Objects.requireNonNull(errorCode, "errorCode must not be null").message());
    }

    /**
     * 使用指定错误码，但根据当前业务上下文覆盖默认提示。
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    /**
     * 全局异常处理器通过该方法取得响应所需的业务码和 HTTP 状态。
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
