package com.kato.pro.langchain.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kato.pro.langchain.domain.knowledge.KnowledgeDoc;
import com.kato.pro.langchain.domain.knowledge.KnowledgeDocStatus;
import com.kato.pro.langchain.domain.knowledge.SourceType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface KnowledgeDocMapper extends TenantAwareBaseMapper<KnowledgeDoc> {

    /** 幂等去重：按 content_hash 查已有文档 */
    default KnowledgeDoc findByContentHash(String contentHash) {
        return selectOne(new QueryWrapper<KnowledgeDoc>().eq("content_hash", contentHash));
    }

    /** 按状态分页查询 */
    default IPage<KnowledgeDoc> selectByStatus(IPage<KnowledgeDoc> page, KnowledgeDocStatus status) {
        return selectPage(page, new QueryWrapper<KnowledgeDoc>()
                .eq("status", status.name())
                .orderByDesc("updated_at"));
    }

    /** 按 sourceType 分页 */
    default IPage<KnowledgeDoc> selectBySourceType(IPage<KnowledgeDoc> page, SourceType sourceType) {
        return selectPage(page, new QueryWrapper<KnowledgeDoc>()
                .eq("source_type", sourceType.name())
                .orderByDesc("updated_at"));
    }

    /** CAS 更新状态机 */
    default int casUpdateStatus(Long docId, KnowledgeDocStatus from, KnowledgeDocStatus to,
                                 String errorMessage, Integer chunkCount, Integer charCount) {
        return update(null, new LambdaUpdateWrapper<KnowledgeDoc>()
                .eq(KnowledgeDoc::getId, docId)
                .eq(KnowledgeDoc::getStatus, from)
                .set(KnowledgeDoc::getStatus, to)
                .set(KnowledgeDoc::getErrorMessage, errorMessage)
                .set(KnowledgeDoc::getChunkCount, chunkCount)
                .set(KnowledgeDoc::getCharCount, charCount));
    }

    /** 按 docId 列表查询（用于 RAG 过滤） */
    default List<KnowledgeDoc> selectByIds(List<Long> ids) {
        return selectBatchIds(ids);
    }
}
