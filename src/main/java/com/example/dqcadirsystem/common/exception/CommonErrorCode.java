package com.example.dqcadirsystem.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 系统级通用错误码。
 *
 * <p>这些错误与具体业务模块无关，可以被任意接口复用。业务模块的专属错误应新建枚举并实现
 * {@link ErrorCode}，不要继续无限扩充本枚举。</p>
 */
public enum CommonErrorCode implements ErrorCode {

    /** 请求成功。 */
    SUCCESS(0, "操作成功", HttpStatus.OK),

    /** 请求参数缺失、类型错误、校验失败或请求体无法解析。 */
    BAD_REQUEST(40000, "请求参数错误", HttpStatus.BAD_REQUEST),

    /** 尚未细分错误码时使用的通用业务错误。 */
    BUSINESS_ERROR(40001, "业务处理失败", HttpStatus.BAD_REQUEST),

    /** 调用方尚未登录或凭证无效；当前仅预留，接入 Spring Security 后使用。 */
    UNAUTHORIZED(40100, "未认证", HttpStatus.UNAUTHORIZED),

    /** 调用方身份有效但没有访问权限；当前仅预留，接入 Spring Security 后使用。 */
    FORBIDDEN(40300, "无权限", HttpStatus.FORBIDDEN),

    /** 请求的接口或资源不存在。 */
    NOT_FOUND(40400, "资源不存在", HttpStatus.NOT_FOUND),

    /** URL 存在，但当前 HTTP 方法不被该接口支持。 */
    METHOD_NOT_ALLOWED(40500, "请求方法不支持", HttpStatus.METHOD_NOT_ALLOWED),

    /** 未被预期和分类的服务器内部异常。 */
    INTERNAL_ERROR(50000, "系统繁忙，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR);

    /** 应用内部业务码。 */
    private final int code;

    /** 可以安全返回给调用方的默认提示。 */
    private final String message;

    /** 与该错误语义匹配的 HTTP 状态码。 */
    private final HttpStatus status;

    /**
     * 枚举实例在类加载时初始化，之后三个属性均不可修改。
     */
    CommonErrorCode(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public HttpStatus status() {
        return status;
    }
}
