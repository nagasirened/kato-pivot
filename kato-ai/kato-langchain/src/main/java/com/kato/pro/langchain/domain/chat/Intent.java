package com.kato.pro.langchain.domain.chat;

/**
 * 用户意图分类（spec §6 步骤 5）。
 *
 *   - TOOL_CALL : 需要调用工具（订单查询、退款申请等结构化操作）
 *   - RAG_ONLY  : 纯知识库问答（不需要写操作；走 RAG → M3 主回复）
 *   - CHITCHAT  : 闲聊（不查知识库；M3 直接回复）
 */
public enum Intent {
    TOOL_CALL, RAG_ONLY, CHITCHAT
}
