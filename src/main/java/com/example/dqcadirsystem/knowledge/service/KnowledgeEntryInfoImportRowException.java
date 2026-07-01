package com.example.dqcadirsystem.knowledge.service;

/**
 * 表示单行可预期的业务校验失败。
 *
 * <p>该异常只在批量导入内部使用，由编排服务转换为 {@code failedList}，不会交给全局异常处理器。
 * 通过抛出运行时异常，可以确保当前行的独立事务在失败时回滚。</p>
 */
public class KnowledgeEntryInfoImportRowException extends RuntimeException {

    public KnowledgeEntryInfoImportRowException(String message) {
        super(message);
    }
}
