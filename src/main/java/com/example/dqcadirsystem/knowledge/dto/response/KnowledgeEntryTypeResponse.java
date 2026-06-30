package com.example.dqcadirsystem.knowledge.dto.response;

import com.example.dqcadirsystem.knowledge.enums.KnowledgeEntryType;

import java.util.Objects;

/**
 * 获取知识条目类型接口中的单个选项。
 *
 * <p>使用独立响应 DTO，而不直接把内部枚举序列化给前端，可以明确控制接口只暴露
 * {@code label} 和 {@code value} 两个字段。以后即使枚举增加内部属性，也不会意外改变接口结构。</p>
 *
 * @param label 前端展示的中文名称
 * @param value 前端提交及数据库保存时使用的稳定编码
 */
public record KnowledgeEntryTypeResponse(String label, String value) {

    /**
     * 将内部枚举转换为对外响应对象。
     *
     * @param entryType 要转换的知识条目类型，不能为空
     * @return 仅包含展示名称和稳定编码的响应对象
     */
    public static KnowledgeEntryTypeResponse from(KnowledgeEntryType entryType) {
        Objects.requireNonNull(entryType, "entryType must not be null");
        return new KnowledgeEntryTypeResponse(entryType.label(), entryType.value());
    }
}
