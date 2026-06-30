package com.example.dqcadirsystem.knowledge.validation;

import com.example.dqcadirsystem.knowledge.enums.KnowledgeFileType;

/**
 * 文件完成全部安全校验后的标准化结果。
 *
 * @param originalFileName 去除客户端路径后的原始文件名
 * @param type 已通过内容特征确认的文件类型
 * @param size 文件大小，单位字节
 */
public record ValidatedKnowledgeFile(
        String originalFileName,
        KnowledgeFileType type,
        long size) {
}
