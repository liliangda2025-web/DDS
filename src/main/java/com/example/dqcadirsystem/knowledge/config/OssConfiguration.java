package com.example.dqcadirsystem.knowledge.config;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.credentials.EnvironmentVariableCredentialsProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 创建并托管阿里云 OSS 客户端。
 *
 * <p>OSS 客户端内部维护连接池，应在整个应用中复用，而不是每次上传都重新创建。
 * {@code destroyMethod = "close"} 确保 Spring 容器关闭时同步释放网络资源。</p>
 */
@Configuration
@EnableConfigurationProperties(OssProperties.class)
public class OssConfiguration {

    /**
     * 构建使用环境变量凭证的 OSS V2 客户端。
     *
     * <p>500 MiB 文件在普通网络下可能需要较长传输时间，因此读写超时设置为十分钟；
     * 连接建立仍使用较短超时，避免网络不可达时请求长时间无响应。</p>
     */
    @Bean(destroyMethod = "close")
    public OSSClient ossClient(OssProperties properties) {
        return OSSClient.newBuilder()
                .region(properties.region())
                .endpoint(properties.endpoint())
                .credentialsProvider(new EnvironmentVariableCredentialsProvider())
                .connectTimeout(Duration.ofSeconds(10))
                .readWriteTimeout(Duration.ofMinutes(10))
                .build();
    }
}
