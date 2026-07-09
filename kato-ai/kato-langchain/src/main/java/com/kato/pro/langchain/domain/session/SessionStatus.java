package com.kato.pro.langchain.domain.session;

/**
 * 会话状态。
 *
 * v1 仅两种状态：ACTIVE（可继续对话）/ ARCHIVED（用户归档，不在列表显示但保留历史）。
 * 后续可加：DELETED（软删除，与 deleted 字段冗余作业务区分）、PINNED（置顶）。
 */
public enum SessionStatus {
    ACTIVE,
    ARCHIVED
}
