package com.example.dqcadirsystem.knowledge.mapper;

import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryPageRequest;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeEntryDetailRow;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeEntryPageRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识条目数据库访问接口。
 *
 * <p>具体 SQL 位于同名 XML 映射文件中，便于维护多个可选查询条件。</p>
 */
@Mapper
public interface KnowledgeEntryMapper {

    /** 查询符合条件的知识条目总数。 */
    long countPage(@Param("request") KnowledgeEntryPageRequest request);

    /** 查询指定分页窗口内的知识条目及其当前有效文件。 */
    List<KnowledgeEntryPageRow> selectPage(@Param("request") KnowledgeEntryPageRequest request);

    /**
     * 按条目 ID 查询正常状态的知识条目详情及其当前有效文件。
     *
     * @return 条目不存在或已经逻辑删除时返回 {@code null}
     */
    KnowledgeEntryDetailRow selectDetail(@Param("entryId") Long entryId);
}
