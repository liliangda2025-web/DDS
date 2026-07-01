package com.example.dqcadirsystem.knowledge.mapper;

import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeFileRow;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeFilePreviewRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 知识文件上传和替换的数据访问接口。
 *
 * <p>所有写操作由 {@code KnowledgeFilePersistenceService} 在同一事务中调用。</p>
 */
@Mapper
public interface KnowledgeFileMapper {

    /** 用于上传 OSS 前的快速存在性检查，不持有数据库行锁。 */
    Long selectActiveEntryId(@Param("entryId") Long entryId);

    /**
     * 在替换事务中锁定正常知识条目，使同一条目的并发上传按获取行锁的顺序提交。
     */
    Long selectActiveEntryIdForUpdate(@Param("entryId") Long entryId);

    /**
     * 将指定条目下所有正常当前文件降级为历史文件。
     *
     * <p>更新“所有当前文件”而非仅更新一条，可同时修复历史遗留的多当前文件异常数据。</p>
     */
    int unsetCurrentFiles(@Param("entryId") Long entryId);

    /** 插入一条已成功上传且为当前文件的数据库记录。 */
    int insertSuccessfulFile(
            @Param("fileId") Long fileId,
            @Param("entryId") Long entryId,
            @Param("originalFileName") String originalFileName,
            @Param("fileType") String fileType,
            @Param("fileSize") Long fileSize,
            @Param("fileUrl") String fileUrl);

    /** 按文件 ID 读取数据库最终值，包括数据库生成的上传时间。 */
    KnowledgeFileRow selectById(@Param("fileId") Long fileId);

    /**
     * 按文件 ID 查询可预览的当前文件。
     *
     * <p>SQL 会同时检查文件、所属知识条目的正常状态以及当前文件标记。替换后的历史文件即使仍保留
     * 数据库记录，也不会通过这个统一预览入口继续暴露。</p>
     */
    KnowledgeFilePreviewRow selectCurrentPreviewById(@Param("fileId") Long fileId);
}
