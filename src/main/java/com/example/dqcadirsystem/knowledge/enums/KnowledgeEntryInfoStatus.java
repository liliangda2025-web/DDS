package com.example.dqcadirsystem.knowledge.enums;

import java.util.Arrays;

/**
 * 知识条目信息完善状态。
 *
 * <p>数值与数据库 {@code knowledge_entry.info_status} 以及接口文档保持一致。</p>
 */
public enum KnowledgeEntryInfoStatus {

    /** 批量上传文件后，业务信息尚未补齐。 */
    PENDING(0, "待补充"),

    /** 关键业务字段已经填写完整。 */
    COMPLETED(1, "已完善");

    private final int value;
    private final String label;

    KnowledgeEntryInfoStatus(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int value() {
        return value;
    }

    public String label() {
        return label;
    }

    /**
     * 把数据库状态值转换为中文名称。
     *
     * <p>数据库出现约定外状态属于数据完整性问题，因此直接抛出异常并交给全局异常处理器记录。</p>
     */
    public static String labelOf(int value) {
        return Arrays.stream(values())
                .filter(status -> status.value == value)
                .map(KnowledgeEntryInfoStatus::label)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知知识条目信息状态: " + value));
    }
}
