package com.example.dqcadirsystem.knowledge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 手工新增知识条目的请求参数。
 *
 * <p>字段长度与 {@code knowledge_entry} 表保持一致，使过长输入在进入数据库前即可得到明确的参数错误。
 * 构造时统一去除文本首尾空白；可选字段只有空白时转换为 {@code null}，避免保存没有业务意义的空字符串。</p>
 */
public record KnowledgeEntryCreateRequest(
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
        String secretLevel) {

    /**
     * 规范化所有文本字段，后续校验、重复判断和入库均使用同一组稳定值。
     */
    public KnowledgeEntryCreateRequest {
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
