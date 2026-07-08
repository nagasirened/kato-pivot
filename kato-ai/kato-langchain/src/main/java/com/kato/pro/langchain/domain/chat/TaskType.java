package com.kato.pro.langchain.domain.chat;

/**
 * 模型调用任务类型。ModelRouter 用这个枚举决策路由到哪个模型。
 *
 * 分类规则：
 *   - SIMPLE_* → 路由到 M2.5（轻量模型，节省成本）
 *   - COMPLEX_* → 路由到 M3（强模型，保证质量）
 *   - EMBEDDING → 路由到 embedding 模型
 *
 * 添加新任务时：先在 ModelRouterConfig 注册 model 映射 + 在 M8 ChatEngine 中调用。
 */
public enum TaskType {

    /** 意图分类（路由到 RAG / 工具 / 闲聊） */
    SIMPLE_CLASSIFICATION,

    /** 长对话超窗历史摘要 */
    SIMPLE_SUMMARIZATION,

    /** 用户问句改写（去除口语化、为检索优化） */
    SIMPLE_QUERY_REWRITE,

    /** 主对话生成（用户可见的最终回答） */
    COMPLEX_CHAT,

    /** 工具调用决策（让模型决定调哪个工具、传什么参数） */
    COMPLEX_TOOL_CALL_DECISION,

    /** 文本向量化 */
    EMBEDDING
}
