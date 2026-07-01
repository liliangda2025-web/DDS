package com.example.dqcadirsystem.knowledge.controller;

import com.example.dqcadirsystem.common.api.ApiResponse;
import com.example.dqcadirsystem.knowledge.dto.response.KnowledgeBatchUploadResponse;
import com.example.dqcadirsystem.knowledge.service.KnowledgeBatchUploadService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库批量文件导入接口。
 *
 * <p>全局 context path 已统一配置为 {@code /api}，本 Controller 只声明模块自身路径。</p>
 */
@RestController
@RequestMapping("/knowledge/files")
public class KnowledgeBatchUploadController {

    private final KnowledgeBatchUploadService batchUploadService;

    public KnowledgeBatchUploadController(KnowledgeBatchUploadService batchUploadService) {
        this.batchUploadService = batchUploadService;
    }

    /**
     * 同步批量上传文件，并返回每个文件的最终成功或失败结果。
     */
    @PostMapping(value = "/batch-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<KnowledgeBatchUploadResponse> batchUpload(
            @RequestPart("entryType") String entryType,
            @RequestPart("files") List<MultipartFile> files) {
        return ApiResponse.success("批量上传完成", batchUploadService.upload(entryType, files));
    }
}
