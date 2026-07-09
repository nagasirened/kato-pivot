package com.kato.pro.langchain.domain.knowledge;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.domain.rag.AutoDocumentLoader;
import com.kato.pro.langchain.domain.rag.EmbeddingIndexer;
import com.kato.pro.langchain.infrastructure.persistence.KnowledgeDocMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 知识库文档服务（CRUD + 触发索引 + 状态机）。
 *
 * 状态机（D7=A）：
 *   create() → PENDING
 *   triggerIndex() → INDEXING → READY / FAILED
 *   delete() → 软删除（BaseEntity.deleted）+ 物理删 vector + chunks
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocService {

    private final KnowledgeDocMapper docMapper;
    private final KnowledgeChunkService chunkService;
    private final AutoDocumentLoader loader;
    private final EmbeddingIndexer indexer;

    /** 创建文档（PENDING 状态）。contentHash 用于幂等去重。 */
    @Transactional
    public KnowledgeDoc create(String title, String fileName, String contentType,
                                byte[] content, SourceType sourceType, String sourceId) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "content 不能为空");
        }
        String hash = sha256(content);
        KnowledgeDoc existing = docMapper.findByContentHash(hash);
        if (existing != null && Integer.valueOf(0).equals(existing.getDeleted())) {
            log.info("Knowledge doc already exists by hash: id={}", existing.getId());
            return existing;
        }
        KnowledgeDoc d = new KnowledgeDoc();
        d.setTitle(title == null ? (fileName == null ? "untitled" : fileName) : title);
        d.setFileName(fileName);
        d.setContentType(contentType);
        d.setContentHash(hash);
        d.setSourceType(sourceType == null ? SourceType.UPLOAD : sourceType);
        d.setSourceId(sourceId);
        d.setStatus(KnowledgeDocStatus.PENDING);
        d.setChunkCount(0);
        d.setCharCount(0);
        docMapper.insert(d);
        log.info("Knowledge doc created: id={} title={}", d.getId(), d.getTitle());
        return d;
    }

    /** 触发索引：PENDING/FAILED → INDEXING → READY/FAILED */
    @Transactional
    public KnowledgeDoc triggerIndex(Long docId, byte[] content, String contentType, String filename) {
        KnowledgeDoc d = getDoc(docId);
        if (d.getStatus() == KnowledgeDocStatus.INDEXING) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文档正在索引中: id=" + docId);
        }
        // 旧 chunks 物理删除
        chunkService.deleteByDocId(docId);
        // 状态：current → INDEXING
        int updated = docMapper.casUpdateStatus(docId,
                d.getStatus(), KnowledgeDocStatus.INDEXING, null, null, null);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "状态机推进失败: id=" + docId);
        }
        try {
            EmbeddingIndexer.IndexResult r = indexer.index(d.getTenantId(), docId, content,
                    contentType, filename, d.getSourceType());
            docMapper.casUpdateStatus(docId, KnowledgeDocStatus.INDEXING,
                    KnowledgeDocStatus.READY, null, r.chunkCount(), r.charCount());
        } catch (Exception e) {
            log.error("Index failed for doc={}", docId, e);
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            if (msg.length() > 1000) msg = msg.substring(0, 1000);
            docMapper.casUpdateStatus(docId, KnowledgeDocStatus.INDEXING,
                    KnowledgeDocStatus.FAILED, msg, null, null);
            if (e instanceof BusinessException be) throw be;
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "索引失败: " + msg);
        }
        return getDoc(docId);
    }

    public KnowledgeDoc getDoc(Long docId) {
        if (docId == null) throw new BusinessException(ErrorCode.PARAM_INVALID, "docId 不能为空");
        KnowledgeDoc d = docMapper.selectById(docId);
        if (d == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "文档不存在: id=" + docId);
        return d;
    }

    public IPage<KnowledgeDoc> listDocs(int page, int size, KnowledgeDocStatus status, SourceType sourceType) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;
        IPage<KnowledgeDoc> p = new Page<>(page, size);
        if (status != null) return docMapper.selectByStatus(p, status);
        if (sourceType != null) return docMapper.selectBySourceType(p, sourceType);
        return docMapper.selectPage(p, null);
    }

    /** 软删除文档（同步物理删除 chunks + vector） */
    @Transactional
    public boolean delete(Long docId) {
        KnowledgeDoc d = getDoc(docId);
        chunkService.deleteByDocId(docId);
        int rows = docMapper.deleteById(docId);
        return rows > 0;
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
