package com.example.dqcadirsystem.knowledge.storage;

import java.io.InputStream;

/**
 * 文件对象存储抽象。
 *
 * <p>知识库业务只依赖该接口，不直接依赖阿里云 SDK。后续若切换私有 Bucket、MinIO 或测试用内存实现，
 * 不需要修改 Controller、事务和数据库持久化逻辑。</p>
 */
public interface ObjectStorageService {

    /**
     * 流式上传一个新对象。
     *
     * @param objectKey OSS 对象键，调用方必须保证唯一
     * @param inputStream 文件输入流，由实现读取但不负责长期持有
     * @param contentLength 文件字节数
     * @param contentType 按真实文件类型确定的标准 MIME
     */
    StoredObject upload(String objectKey, InputStream inputStream, long contentLength, String contentType);

    /**
     * 删除指定对象，主要用于“OSS 已成功但数据库事务失败”时的补偿。
     */
    void delete(String objectKey);
}
