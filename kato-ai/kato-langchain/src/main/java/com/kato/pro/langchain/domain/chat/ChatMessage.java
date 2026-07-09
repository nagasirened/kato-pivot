package com.kato.pro.langchain.domain.chat;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kato.pro.langchain.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 聊天消息实体。一次对话中的单条消息。
 *
 * 角色：USER（用户输入）/ ASSISTANT（模型回复）/ SYSTEM（系统提示注入）/ TOOL（工具调用结果）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message")
public class ChatMessage extends BaseEntity {

    public enum Role { USER, ASSISTANT, SYSTEM, TOOL }

    /** 所属会话 ID */
    private Long sessionId;

    /** 消息角色 */
    @EnumValue
    private Role role;

    /** 消息文本内容 */
    private String content;

    /** 消息 token 消耗（用于 context window 跟踪，可空） */
    private Integer tokenCount;

    /** 产生此消息的模型名（USER 消息为空；ASSISTANT 消息记录 m2.5 / m3） */
    private String modelName;

    /** 链路追踪 ID（来自 M6 TraceContext） */
    private String traceId;

    /** 工具调用 JSON（仅 TOOL/ASSISTANT 含 tool_call 的消息；可空） */
    @TableField("tool_call_json")
    private String toolCallJson;

    /** RAG 引用文档 ID 列表（逗号分隔，仅 RAG 类回答；可空） */
    @TableField("citation_doc_ids")
    private String citationDocIds;
}
