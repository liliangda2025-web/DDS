package com.example.dqcadirsystem.knowledge.storage;

/**
 * 对象存储适配层异常。
 *
 * <p>该异常保留 OSS SDK 的原始异常作为 cause，供服务层记录完整日志；Controller 最终只会得到
 * 安全的业务提示，不会把 Bucket、请求 ID 或 SDK 内部信息泄露给前端。</p>
 */
public class ObjectStorageException extends RuntimeException {

    public ObjectStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
