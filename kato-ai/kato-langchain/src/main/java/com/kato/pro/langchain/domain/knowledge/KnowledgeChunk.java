package com.kato.pro.langchain.domain.knowledge;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kato.pro.langchain.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库分段（vector_ref 由 VectorStore 内部生成，InMemory 自增）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_chunk")
public class KnowledgeChunk extends BaseEntity {

    /** 所属文档 ID */
    private Long docId;

    /** 分段顺序（0-based） */
    private Integer chunkIndex;

    /** 分段文本 */
    private String content;

    /** Embedding 模型名 */
    @TableField("embedding_model")
    private String embeddingModel;

    /** 向量库内部 ID（v1 = InMemoryVectorStore 自增） */
    @TableField("vector_ref")
    private Long vectorRef;

    /** 该段 token 数（用于 R3 控制） */
    @TableField("token_count")
    private Integer tokenCount;
}
