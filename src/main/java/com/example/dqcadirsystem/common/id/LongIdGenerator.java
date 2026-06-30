package com.example.dqcadirsystem.common.id;

/**
 * 生成数据库 BIGINT 主键的统一接口。
 *
 * <p>业务 Service 依赖该抽象而不是具体算法，便于单元测试固定返回值，也便于将来把本地雪花算法替换为
 * 数据库号段或独立发号服务，而不修改业务代码。</p>
 */
public interface LongIdGenerator {

    /**
     * 生成一个正数、在当前部署范围内唯一的 64 位整数 ID。
     */
    long nextId();
}
