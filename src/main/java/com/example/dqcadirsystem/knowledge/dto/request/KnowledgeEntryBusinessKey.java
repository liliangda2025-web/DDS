package com.example.dqcadirsystem.knowledge.dto.request;

/**
 * 知识条目业务唯一键的只读视图。
 *
 * <p>新增和修改请求都包含类型、编码和版本。通过这个小接口复用重复检查，同时保持 Mapper 参数的类型安全，
 * 不要求两个请求 DTO 继承或共用全部字段。</p>
 */
public interface KnowledgeEntryBusinessKey {

    String entryType();

    String entryCode();

    String version();
}
