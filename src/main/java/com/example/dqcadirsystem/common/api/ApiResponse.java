package com.example.dqcadirsystem.common.api;

import com.example.dqcadirsystem.common.exception.CommonErrorCode;
import com.example.dqcadirsystem.common.exception.ErrorCode;

import java.util.Objects;

/**
 * 所有 JSON 接口共用的统一响应结构。
 *
 * <p>这里使用 Java {@code record}，是因为响应对象只负责承载数据，不需要可变状态。
 * Jackson 会把该对象稳定序列化为 {@code code}、{@code message}、{@code data} 三个字段。
 * Controller 应显式返回 {@code ApiResponse<T>}，这样从方法签名就能看出接口是否遵循统一协议。</p>
 *
 * <p>{@code code} 是应用内部业务码，不等同于 HTTP 状态码。HTTP 状态码由异常处理器通过
 * {@link org.springframework.http.ResponseEntity} 单独设置。</p>
 *
 * @param code    应用业务码；成功固定为 200，失败使用五位错误码
 * @param message 可直接展示或供前端判断的提示信息
 * @param data    实际响应数据；没有数据时仍会序列化为 {@code null}
 * @param <T>     响应数据的具体类型
 */
public record ApiResponse<T>(int code, String message, T data) {

    /**
     * 创建没有响应数据的成功结果。
     *
     * <p>适合删除、启停、提交等只需要告诉调用方“操作成功”的接口。</p>
     */
    public static ApiResponse<Void> success() {
        return success(null);
    }

    /**
     * 使用默认成功提示创建成功结果。
     *
     * @param data 需要返回给调用方的业务数据
     * @param <T>  数据类型，由传入参数自动推断
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(CommonErrorCode.SUCCESS.message(), data);
    }

    /**
     * 使用自定义成功提示创建成功结果。
     *
     * <p>业务码仍然固定为 200，只改变提示语。例如某些提交接口可以返回“提交成功”。</p>
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(CommonErrorCode.SUCCESS.code(), message, data);
    }

    /**
     * 使用错误码中预定义的提示信息创建失败结果。
     *
     * <p>通常由全局异常处理器调用，业务代码一般只需要抛出 {@code BusinessException}。</p>
     */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        // 尽早阻止错误码为空，避免真正处理异常时再次产生难以定位的空指针异常。
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        return failure(errorCode, errorCode.message());
    }

    /**
     * 使用指定错误码和自定义提示信息创建失败结果。
     *
     * <p>适合错误类型固定、但提示需要包含业务上下文的场景。</p>
     */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        // 失败响应不携带业务数据，因此 data 明确为 null，保证协议结构不变。
        return new ApiResponse<>(errorCode.code(), message, null);
    }
}
