package com.kato.pro.langchain.domain.rag;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.domain.embedding.EmbeddingModel;
import com.kato.pro.langchain.domain.embedding.EmbeddingRequest;
import com.kato.pro.langchain.domain.embedding.EmbeddingResponse;
import com.kato.pro.langchain.domain.knowledge.IndexedChunk;
import com.kato.pro.langchain.domain.knowledge.KnowledgeChunk;
import com.kato.pro.langchain.domain.knowledge.SourceType;
import com.kato.pro.langchain.infrastructure.persistence.KnowledgeChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Embedding 索引器：解析 → 分段 → Embedding → 写库 → 入 vector。
 *
 * 调用方：KnowledgeDocService.triggerIndex(...)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingIndexer {

    private final AutoDocumentLoader loader;
    private final DocumentSplitter splitter;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final KnowledgeChunkMapper chunkMapper;

    /**
     * 把 raw bytes 解析 → 分段 → Embedding → 写库 → 入 vector。
     * @return (chunkCount, charCount)
     */
    @Transactional
    public IndexResult index(Long tenantId, Long docId, byte[] content,
                              String contentType, String filename, SourceType sourceType) {
        if (docId == null) throw new BusinessException(ErrorCode.PARAM_INVALID, "docId 不能为空");
        String text;
        try {
            text = loader.load(content, contentType, filename);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文档解析失败: " + e.getMessage());
        }
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文档内容为空");
        }
        List<String> chunks = splitter.split(text);
        if (chunks.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文档分段后为空（可能全是空白）");
        }

        // 调 Embedding（batch）
        EmbeddingRequest req = EmbeddingRequest.builder().inputs(chunks).build();
        EmbeddingResponse resp = embeddingModel.embed(req).block();
        if (resp == null || resp.getVectors() == null || resp.getVectors().size() != chunks.size()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Embedding 模型返回向量数(" +
                    (resp == null || resp.getVectors() == null ? 0 : resp.getVectors().size()) +
                    ") 与 chunks 数(" + chunks.size() + ") 不一致");
        }

        // 写库 + 写 vector
        List<float[]> vectors = resp.getVectors();
        for (int i = 0; i < chunks.size(); i++) {
            String c = chunks.get(i);
            float[] v = vectors.get(i);

            long vectorRef = vectorStore.nextVectorRef();
            KnowledgeChunk kc = new KnowledgeChunk();
            kc.setDocId(docId);
            kc.setChunkIndex(i);
            kc.setContent(c);
            kc.setEmbeddingModel(embeddingModel.modelName());
            kc.setVectorRef(vectorRef);
            kc.setTokenCount(c.length() / 4); // chars/4 估算
            chunkMapper.insert(kc);

            vectorStore.add(new IndexedChunk(tenantId, kc.getId(), docId,
                    sourceType == null ? null : sourceType.code(), i, c, v));
        }
        log.info("Indexed doc={} chunks={} chars={}", docId, chunks.size(), text.length());
        return new IndexResult(chunks.size(), text.length());
    }

    public record IndexResult(int chunkCount, int charCount) {}
}
