package com.kato.pro.langchain.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kato.pro.langchain.domain.knowledge.KnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface KnowledgeChunkMapper extends TenantAwareBaseMapper<KnowledgeChunk> {

    /** 按 docId 查所有 chunks（按 chunkIndex 升序） */
    default List<KnowledgeChunk> selectByDocId(Long docId) {
        return selectList(new QueryWrapper<KnowledgeChunk>()
                .eq("doc_id", docId)
                .orderByAsc("chunk_index"));
    }

    /** 按 docId 集合查 chunks */
    default List<KnowledgeChunk> selectByDocIds(List<Long> docIds) {
        if (docIds == null || docIds.isEmpty()) return List.of();
        return selectList(new QueryWrapper<KnowledgeChunk>().in("doc_id", docIds));
    }

    /** 删除 doc 下的所有 chunks（物理删除 — 软删除在 SELECT 阶段由 MyBatis-Plus 自动加） */
    default int deleteByDocId(Long docId) {
        return delete(new QueryWrapper<KnowledgeChunk>().eq("doc_id", docId));
    }
}
