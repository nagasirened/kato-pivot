package com.kato.pro.langchain.domain.knowledge;

/**
 * 知识库文档索引状态机（D7=A）。
 *
 *   PENDING   — 上传后等待触发索引
 *   INDEXING  — 正在分段/向量化
 *   READY     — 索引完成，可检索
 *   FAILED    — 索引失败（保留错误日志）
 */
public enum KnowledgeDocStatus {
    PENDING,
    INDEXING,
    READY,
    FAILED
}
