package com.example.dqcadirsystem.knowledge.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 知识条目类型枚举。
 *
 * <p>知识条目类型属于稳定的业务字典，而不是用户在运行期间维护的数据，因此当前阶段直接使用枚举，
 * 不额外建立字典表。新增、修改、查询和批量导入等后续接口都应复用本枚举，避免在不同业务代码中
 * 重复书写字符串并产生拼写不一致。</p>
 *
 * <p>枚举常量的声明顺序也是“获取知识条目类型”接口的返回顺序。调整顺序会影响前端下拉框展示，
 * 因此除非接口契约明确变更，否则不要随意重新排列。</p>
 */
public enum KnowledgeEntryType {

    /** 图纸类知识资源。 */
    DRAWING("图纸库"),

    /** 程序、流程或规则的生效准则。 */
    PROGRAM_RULE("程序生效准则"),

    /** 法律、法规及规范类知识资源。 */
    LAW("法律法规"),

    /** 已发生问题及其处理过程形成的历史案例。 */
    CASE("历史案例");

    /** 面向用户展示的中文名称。 */
    private final String label;

    KnowledgeEntryType(String label) {
        this.label = label;
    }

    /**
     * 返回接口和数据库中使用的稳定类型编码。
     *
     * <p>直接使用枚举常量名作为编码，可以保证 Java 类型、接口值和数据库 {@code entry_type}
     * 三者保持一致。</p>
     */
    public String value() {
        return name();
    }

    /**
     * 返回前端下拉框或页面中展示的中文名称。
     */
    public String label() {
        return label;
    }

    /**
     * 按接口和数据库中的稳定编码查找知识条目类型。
     *
     * <p>返回 {@link Optional} 而不是直接抛出异常，调用方可以根据使用场景决定把未知值视为请求参数错误，
     * 还是数据库数据完整性错误。</p>
     *
     * @param value 待查找的类型编码
     * @return 匹配的枚举；编码为空或不存在时返回空
     */
    public static Optional<KnowledgeEntryType> fromValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(type -> type.value().equals(value))
                .findFirst();
    }

    /**
     * 判断给定编码是否为系统支持的知识条目类型。
     */
    public static boolean isValid(String value) {
        return fromValue(value).isPresent();
    }
}
