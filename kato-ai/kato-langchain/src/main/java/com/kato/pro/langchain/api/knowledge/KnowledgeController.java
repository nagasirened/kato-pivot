package com.kato.pro.langchain.api.knowledge;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kato.pro.langchain.api.chat.dto.PageResult;
import com.kato.pro.langchain.api.knowledge.dto.KnowledgeDocVO;
import com.kato.pro.langchain.api.knowledge.dto.SearchRequest;
import com.kato.pro.langchain.api.knowledge.dto.SearchResultVO;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.result.Result;
import com.kato.pro.langchain.domain.knowledge.KnowledgeDoc;
import com.kato.pro.langchain.domain.knowledge.KnowledgeDocService;
import com.kato.pro.langchain.domain.knowledge.KnowledgeDocStatus;
import com.kato.pro.langchain.domain.knowledge.RagPipeline;
import com.kato.pro.langchain.domain.knowledge.ScoredChunk;
import com.kato.pro.langchain.domain.knowledge.SourceType;
import com.kato.pro.langchain.domain.rag.QueryRewriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * 知识库 REST API（D6=A — 完整 CRUD + 上传 + 检索）。
 *
 *   POST   /api/v1/knowledge/docs          — 上传文档（multipart）+ 触发索引
 *   GET    /api/v1/knowledge/docs          — 分页查询
 *   DELETE /api/v1/knowledge/docs/{id}     — 软删除
 *   POST   /api/v1/knowledge/search        — 检索（debug/admin 用）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeDocService docService;
    private final RagPipeline ragPipeline;
    private final QueryRewriter queryRewriter;

    @PostMapping("/docs")
    public Result<KnowledgeDocVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "sourceType", defaultValue = "UPLOAD") String sourceType,
            @RequestParam(value = "sourceId", required = false) String sourceId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "file 不能为空");
        }
        SourceType st;
        try { st = SourceType.fromCode(sourceType); } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "sourceType 无效: " + sourceType);
        }
        byte[] bytes;
        try { bytes = file.getBytes(); } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "读取文件失败: " + e.getMessage());
        }
        KnowledgeDoc created = docService.create(title, file.getOriginalFilename(),
                file.getContentType(), bytes, st, sourceId);
        KnowledgeDoc indexed = docService.triggerIndex(created.getId(), bytes,
                file.getContentType(), file.getOriginalFilename());
        return Result.ok(KnowledgeDocVO.from(indexed));
    }

    @GetMapping("/docs")
    public Result<PageResult<KnowledgeDocVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sourceType) {
        KnowledgeDocStatus st = status == null ? null : KnowledgeDocStatus.valueOf(status);
        SourceType sty = sourceType == null ? null : SourceType.fromCode(sourceType);
        IPage<KnowledgeDoc> p = docService.listDocs(page, size, st, sty);
        return Result.ok(PageResult.of(p, KnowledgeDocVO::from));
    }

    @DeleteMapping("/docs/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean ok = docService.delete(id);
        return Result.ok(ok);
    }

    @PostMapping("/search")
    public Result<SearchResultVO> search(@RequestBody SearchRequest req) {
        if (req == null || req.getQuery() == null || req.getQuery().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "query 不能为空");
        }
        String rewritten = queryRewriter.rewrite(req.getQuery());
        Set<Long> docIds = req.getDocIds() == null ? null : Set.copyOf(req.getDocIds());
        Set<String> sourceTypes = req.getSourceTypes() == null ? null : Set.copyOf(req.getSourceTypes());
        List<ScoredChunk> hits = ragPipeline.search(req.getQuery(), req.getTopK(),
                req.getMinScore(), docIds, sourceTypes);
        return Result.ok(SearchResultVO.build(req.getQuery(), rewritten, hits));
    }
}
