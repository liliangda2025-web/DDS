package com.example.dqcadirsystem.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 阿里云 OSS 的非敏感连接配置。
 *
 * <p>这里只保存地域、Bucket 和访问域名。AccessKey ID 与 AccessKey Secret 不属于配置对象，
 * OSS SDK 会直接从 {@code OSS_ACCESS_KEY_ID} 和 {@code OSS_ACCESS_KEY_SECRET} 环境变量读取，
 * 从结构上避免开发者误把密钥提交到配置文件。</p>
 */
@ConfigurationProperties(prefix = "app.oss")
public record OssProperties(
        String region,
        String endpoint,
        String bucket,
        String publicBaseUrl,
        String keyPrefix) {
}
