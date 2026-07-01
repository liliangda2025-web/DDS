package com.example.dqcadirsystem.knowledge.mapper;

import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryCreateRequest;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryBusinessKey;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryPageRequest;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeEntryUpdateRequest;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeSupplementTemplateExportRequest;
import com.example.dqcadirsystem.knowledge.dto.request.KnowledgeSearchRequest;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeEntryDetailRow;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeEntryInfoImportFileRow;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeEntryPageRow;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeSupplementTemplateRow;
import com.example.dqcadirsystem.knowledge.mapper.model.KnowledgeSearchRow;
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

    /**
     * 批量文件导入成功后插入一条待补充占位知识条目。
     *
     * <p>操作人字段当前阶段保持为空；其余非必填业务字段也保持 {@code NULL}，后续由 Excel
     * 批量导入知识条目信息接口补全。</p>
     */
    int insertBatchPlaceholderEntry(
            @Param("entryId") long entryId,
            @Param("entryType") String entryType,
            @Param("entryCode") String entryCode,
            @Param("title") String title);

    /**
     * 查询并锁定指定 ID 的正常知识条目，锁持续到当前事务结束。
     *
     * @return 正常条目 ID；不存在或已删除时返回 {@code null}
     */
    Long selectActiveIdForUpdate(@Param("entryId") Long entryId);

    /**
     * 更新正常状态知识条目的业务元数据。
     *
     * @return 数据库报告的影响行数；目标不存在时为 0
     */
    int updateEntry(@Param("entryId") Long entryId,
                    @Param("request") KnowledgeEntryUpdateRequest request);

    /**
     * 锁定批量信息导入的目标条目并返回其完善状态。
     *
     * @return 正常条目的 info_status；条目不存在或已删除时返回 {@code null}
     */
    Integer selectActiveInfoStatusForUpdate(@Param("entryId") Long entryId);

    /**
     * 查询指定条目下与模板 ID 对应的当前成功文件，并返回系统列核对所需字段。
     */
    KnowledgeEntryInfoImportFileRow selectValidImportFile(
            @Param("entryId") Long entryId,
            @Param("fileId") Long fileId);

    /**
     * 逻辑删除一条正常知识条目，并把删除标记更新为该条目自身 ID。
     *
     * @return 删除成功为 1，条目不存在或已删除为 0
     */
    int logicalDeleteEntry(@Param("entryId") Long entryId);

    /**
     * 将指定知识条目下的全部正常文件标记为删除。
     *
     * @return 实际失效的文件数量；条目没有文件时可以为 0
     */
    int logicalDeleteFilesByEntryId(@Param("entryId") Long entryId);

    /**
     * 精确查询指定 ID 对应的可导出待补充记录。
     *
     * <p>调用方会比较查询数量与请求 ID 数量；任一条目不符合导出条件时，整个导出请求失败。</p>
     */
    List<KnowledgeSupplementTemplateRow> selectPendingSupplementByEntryIds(
            @Param("entryIds") List<Long> entryIds);

    /**
     * 按类型、文件名和上传时间筛选可导出待补充记录。
     *
     * @param limit 实际查询上限；业务层传 5001，用于识别是否超过 5000 条导出上限
     */
    List<KnowledgeSupplementTemplateRow> selectPendingSupplementByFilter(
            @Param("request") KnowledgeSupplementTemplateExportRequest request,
            @Param("limit") int limit);

    /** 统计符合关键词、可正常打开当前文件的已完善知识条目数量。 */
    long countSearch(@Param("request") KnowledgeSearchRequest request);

    /** 按更新时间倒序查询当前分页中的知识检索卡片。 */
    List<KnowledgeSearchRow> selectSearch(@Param("request") KnowledgeSearchRequest request);
}
