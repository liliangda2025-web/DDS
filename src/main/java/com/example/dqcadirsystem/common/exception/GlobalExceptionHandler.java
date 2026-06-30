package com.example.dqcadirsystem.common.exception;

import com.example.dqcadirsystem.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Optional;

/**
 * Web 层全局异常处理器。
 *
 * <p>{@link RestControllerAdvice} 会让 Spring MVC 在 Controller 抛出异常后，按照异常的具体类型寻找
 * 对应的 {@link ExceptionHandler} 方法，并将返回值直接序列化为 JSON。这样 Controller 和业务层只关注
 * 正常流程，异常响应的结构、状态码和日志策略集中在一个地方维护。</p>
 *
 * <p>处理顺序由 Spring 根据异常类型的精确程度决定：具体异常会进入对应方法，最后才由
 * {@link #handleUnexpectedException(Exception, HttpServletRequest)} 兜底。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 使用当前类作为日志分类，便于在线上集中检索接口异常。 */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务代码主动抛出的可预期异常。
     *
     * <p>业务异常使用 warn 级别并记录请求方法、路径和业务码，便于排查，但不按系统故障记录完整堆栈。</p>
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception, HttpServletRequest request) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn("Business exception: code={}, method={}, uri={}, message={}",
                errorCode.code(), request.getMethod(), request.getRequestURI(), exception.getMessage());
        return failure(errorCode, exception.getMessage());
    }

    /**
     * 处理 {@code @Valid @RequestBody} 触发的 DTO 字段校验异常。
     *
     * <p>按照已确定的接口协议，只返回第一条校验信息：优先字段错误，其次对象级错误；如果校验器没有
     * 提供具体信息，则退回通用的参数错误提示。</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception) {
        // FieldError 对应 @NotBlank 等字段注解，GlobalError 对应对象级联合校验。
        String message = Optional.ofNullable(exception.getBindingResult().getFieldError())
                .map(FieldError::getDefaultMessage)
                .orElseGet(() -> Optional.ofNullable(exception.getBindingResult().getGlobalError())
                        .map(MessageSourceResolvable::getDefaultMessage)
                        .orElse(CommonErrorCode.BAD_REQUEST.message()));
        return failure(CommonErrorCode.BAD_REQUEST, message);
    }

    /**
     * 处理 Spring MVC 方法参数校验异常。
     *
     * <p>该异常通常来自 Controller 的 {@code @RequestParam}、{@code @PathVariable} 等参数上直接声明的
     * {@code @Min}、{@code @NotBlank} 等约束。它与请求体 DTO 的校验异常类型不同，因此需要单独处理。</p>
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(
            HandlerMethodValidationException exception) {
        // getAllErrors() 同时覆盖各方法参数的校验结果，这里只选取第一条非空提示。
        String message = exception.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(CommonErrorCode.BAD_REQUEST.message());
        return failure(CommonErrorCode.BAD_REQUEST, message);
    }

    /**
     * 处理 Jakarta Validation 在非 MVC 参数绑定流程中抛出的约束异常。
     *
     * <p>例如方法级校验代理或业务服务参数校验可能抛出该异常。保留该处理可以让不同校验入口的响应
     * 仍然遵循同一协议。</p>
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(CommonErrorCode.BAD_REQUEST.message());
        return failure(CommonErrorCode.BAD_REQUEST, message);
    }

    /**
     * 处理必填查询参数缺失，例如接口要求 {@code ?name=...}，但请求中完全没有 name。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestParameter(
            MissingServletRequestParameterException exception) {
        return failure(CommonErrorCode.BAD_REQUEST, "缺少必填参数: " + exception.getParameterName());
    }

    /**
     * 处理请求参数类型转换失败，例如 Integer 参数收到字符串 {@code abc}。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception) {
        return failure(CommonErrorCode.BAD_REQUEST, "参数类型错误: " + exception.getName());
    }

    /**
     * 处理请求体无法反序列化的情况，例如 JSON 缺少引号、括号不完整或字段类型不兼容。
     *
     * <p>不返回 Jackson 的原始解析信息，避免把 Java 类型和内部实现细节暴露给调用方。</p>
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable() {
        return failure(CommonErrorCode.BAD_REQUEST, "请求体格式错误");
    }

    /**
     * 处理找不到接口或静态资源的请求。
     *
     * <p>Spring 的不同请求映射配置可能分别抛出这两种异常，因此在同一个方法中统一转换为 404。</p>
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound() {
        return failure(CommonErrorCode.NOT_FOUND);
    }

    /**
     * 处理 URL 存在但 HTTP 方法不匹配的情况，例如只允许 GET 的接口收到 POST 请求。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported() {
        return failure(CommonErrorCode.METHOD_NOT_ALLOWED);
    }

    /**
     * 处理所有尚未被前述方法识别的系统异常。
     *
     * <p>日志中保留完整异常堆栈供开发人员定位；响应中只返回固定的 50000 和安全提示，绝不直接返回
     * {@link Exception#getMessage()}，防止 SQL、文件路径、密钥等内部信息泄露。</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception exception, HttpServletRequest request) {
        log.error("Unexpected exception: method={}, uri={}",
                request.getMethod(), request.getRequestURI(), exception);
        return failure(CommonErrorCode.INTERNAL_ERROR);
    }

    /**
     * 使用错误码中的默认提示构建响应，同时设置对应 HTTP 状态码。
     */
    private ResponseEntity<ApiResponse<Void>> failure(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.status()).body(ApiResponse.failure(errorCode));
    }

    /**
     * 使用自定义安全提示构建响应，同时保留错误码定义的 HTTP 状态码。
     */
    private ResponseEntity<ApiResponse<Void>> failure(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.status()).body(ApiResponse.failure(errorCode, message));
    }
}
