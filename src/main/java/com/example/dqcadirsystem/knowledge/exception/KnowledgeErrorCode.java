package com.example.dqcadirsystem.knowledge.exception;

import com.example.dqcadirsystem.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 知识库模块专属错误码。
 *
 * <p>通用参数错误和资源不存在继续使用 {@code CommonErrorCode}；只有文件存储服务异常这类
 * 知识库特有语义放在本枚举中，防止通用错误码枚举不断膨胀。</p>
 */
public enum KnowledgeErrorCode implements ErrorCode {

    /** OSS 上传失败，网关语义表示当前服务依赖的外部存储暂时不可用。 */
    FILE_UPLOAD_FAILED(50200, "文件上传失败，请稍后重试", HttpStatus.BAD_GATEWAY);

    private final int code;
    private final String message;
    private final HttpStatus status;

    KnowledgeErrorCode(int code, String message, HttpStatus status) {
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
