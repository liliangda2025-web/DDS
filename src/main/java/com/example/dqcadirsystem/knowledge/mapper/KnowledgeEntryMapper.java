package com.example.dqcadirsystem.knowledge.mapper;

import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryCreateRequest;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryBusinessKey;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryPageRequest;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryUpdateRequest;
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

    /**
     * 查询相同类型、编码和版本的正常知识条目数量。
     *
     * @param excludedEntryId 修改场景传当前条目 ID 以排除自身；新增场景传 {@code null}
     */
    int countActiveByBusinessKey(@Param("request") KnowledgeEntryBusinessKey request,
                                 @Param("excludedEntryId") Long excludedEntryId);

    /**
     * 新增一条手工录入的知识条目。
     *
     * @return 实际插入行数，正常情况下固定为 1
     */
    int insertEntry(@Param("entryId") long entryId,
                    @Param("request") KnowledgeEntryCreateRequest request);

    /** 查询指定 ID 的正常知识条目是否存在。 */
    int countActiveById(@Param("entryId") Long entryId);

    /**
     * 更新正常状态知识条目的业务元数据。
     *
     * @return 数据库报告的影响行数；目标不存在时为 0
     */
    int updateEntry(@Param("entryId") Long entryId,
                    @Param("request") KnowledgeEntryUpdateRequest request);
}
