package com.kato.pro.langchain.domain.knowledge;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.domain.rag.VectorStore;
import com.kato.pro.langchain.infrastructure.persistence.KnowledgeChunkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeChunkService {

    private final KnowledgeChunkMapper chunkMapper;
    private final VectorStore vectorStore;

    public List<KnowledgeChunk> listByDocId(Long docId) {
        if (docId == null) throw new BusinessException(ErrorCode.PARAM_INVALID, "docId 不能为空");
        return chunkMapper.selectByDocId(docId);
    }

    /** 物理删除 chunks + vector。状态机推进前由 KnowledgeDocService 调用。 */
    @Transactional
    public int deleteByDocId(Long docId) {
        if (docId == null) return 0;
        int rows = chunkMapper.deleteByDocId(docId);
        vectorStore.deleteByDocId(/* tenantId 占位：v1 简化，不带 tenantId */ null, docId);
        return rows;
    }
}
