package com.example.dqcadirsystem.knowledge.service;

import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeEntryTypeResponse;
import com.example.dqcadirsystem.knowledge.enums.KnowledgeEntryType;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 知识条目类型查询服务。
 *
 * <p>虽然当前逻辑只是读取固定枚举，仍通过服务层完成枚举到响应 DTO 的转换，使 Controller 只负责
 * HTTP 协议。以后如果类型来源调整为配置中心或数据库，Controller 的公开接口无需随之修改。</p>
 */
@Service
public class KnowledgeEntryTypeService {

    /**
     * 获取系统支持的全部知识条目类型。
     *
     * <p>{@link KnowledgeEntryType#values()} 按枚举声明顺序返回数据；{@link java.util.stream.Stream#toList()}
     * 生成不可修改的列表，防止调用方无意中增删固定类型。</p>
     *
     * @return 按约定顺序排列的知识条目类型选项
     */
    public List<KnowledgeEntryTypeResponse> listEntryTypes() {
        return Arrays.stream(KnowledgeEntryType.values())
                .map(KnowledgeEntryTypeResponse::from)
                .toList();
    }
}
