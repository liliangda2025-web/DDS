package com.example.dqcadirsystem.knowledge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 修改知识条目信息的请求参数。
 *
 * <p>修改接口与新增接口拥有相同的业务字段，但使用独立 DTO，避免未来两个接口的字段策略发生变化时互相影响。
 * 字段长度与数据库保持一致，所有文本在校验和入库前都会去除首尾空白。</p>
 */
public record KnowledgeEntryUpdateRequest(
        @NotBlank(message = "知识条目类型不能为空")
        @Size(max = 50, message = "知识条目类型长度不能超过50个字符")
        String entryType,

        @NotBlank(message = "条目编码不能为空")
        @Size(max = 100, message = "条目编码长度不能超过100个字符")
        String entryCode,

        @NotBlank(message = "标题不能为空")
        @Size(max = 255, message = "标题长度不能超过255个字符")
        String title,

        @Size(max = 500, message = "关键词长度不能超过500个字符")
        String keywords,

        @NotBlank(message = "版本不能为空")
        @Size(max = 50, message = "版本长度不能超过50个字符")
        String version,

        @Size(max = 255, message = "所属项目长度不能超过255个字符")
        String projectName,

        LocalDate releaseDate,

        @Size(max = 255, message = "系统来源长度不能超过255个字符")
        String systemSource,

        @Size(max = 100, message = "专业代码长度不能超过100个字符")
        String professionCode,

        @Size(max = 100, message = "编写人长度不能超过100个字符")
        String authorName,

        @Size(max = 50, message = "密级长度不能超过50个字符")
        String secretLevel) implements KnowledgeEntryBusinessKey {

    /** 统一规范化文本，保证重复校验和更新 SQL 使用完全相同的值。 */
    public KnowledgeEntryUpdateRequest {
        entryType = trimToNull(entryType);
        entryCode = trimToNull(entryCode);
        title = trimToNull(title);
        keywords = trimToNull(keywords);
        version = trimToNull(version);
        projectName = trimToNull(projectName);
        systemSource = trimToNull(systemSource);
        professionCode = trimToNull(professionCode);
        authorName = trimToNull(authorName);
        secretLevel = trimToNull(secretLevel);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
