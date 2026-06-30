package com.example.dqcadirsystem.knowledge.storage;

/**
 * 对象成功写入存储后的定位信息。
 *
 * @param objectKey OSS Bucket 内部用于删除、覆盖控制的对象键
 * @param publicUrl 当前公共读 Bucket 对外提供的永久访问地址
 */
public record StoredObject(String objectKey, String publicUrl) {
}
