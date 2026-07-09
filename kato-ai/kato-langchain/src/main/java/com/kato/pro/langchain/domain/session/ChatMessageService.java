package com.kato.pro.langchain.domain.session;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.trace.TraceContext;
import com.kato.pro.langchain.domain.chat.ChatMessage;
import com.kato.pro.langchain.infrastructure.persistence.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 消息服务。所有 append* 方法负责：构造实体 + 写入 DB + 触发 session touch。
 *
 * 注意：append 方法不传 userId — 从 TenantContext 读（与 ChatSessionService 一致）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    public static final int MAX_CONTENT_LENGTH = 32_000; // 单条消息 32K chars

    private final ChatMessageMapper messageMapper;

    /** 追加 USER 消息 */
    @Transactional
    public ChatMessage appendUserMessage(Long sessionId, String content) {
        return append(sessionId, ChatMessage.Role.USER, validateContent(content), null, null, null, null);
    }

    /** 追加 ASSISTANT 消息（带模型名 + token 消耗） */
    @Transactional
    public ChatMessage appendAssistantMessage(Long sessionId, String content, String modelName, Integer tokenCount) {
        return append(sessionId, ChatMessage.Role.ASSISTANT, validateContent(content), modelName, tokenCount, null, null);
    }

    /** 追加 SYSTEM 消息（用于 M4 摘要注入） */
    @Transactional
    public ChatMessage appendSystemMessage(Long sessionId, String content) {
        return append(sessionId, ChatMessage.Role.SYSTEM, validateContent(content), null, null, null, null);
    }

    /** 追加 TOOL 消息（用于 M7 工具调用结果） */
    @Transactional
    public ChatMessage appendToolMessage(Long sessionId, String content, String toolCallJson) {
        return append(sessionId, ChatMessage.Role.TOOL, validateContent(content), null, null, toolCallJson, null);
    }

    private ChatMessage append(Long sessionId, ChatMessage.Role role, String content,
                                String modelName, Integer tokenCount,
                                String toolCallJson, String citationDocIds) {
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "sessionId 不能为空");
        }
        ChatMessage m = new ChatMessage();
        m.setSessionId(sessionId);
        m.setRole(role);
        m.setContent(content);
        m.setModelName(modelName);
        m.setTokenCount(tokenCount);
        m.setToolCallJson(toolCallJson);
        m.setCitationDocIds(citationDocIds);
        m.setTraceId(TraceContext.current()); // 关联 traceId
        messageMapper.insert(m);
        log.debug("Message appended: session={}, role={}, traceId={}", sessionId, role, m.getTraceId());
        return m;
    }

    /** 分页查询某会话的消息 */
    public IPage<ChatMessage> listSessionMessages(Long sessionId, int page, int size) {
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "sessionId 不能为空");
        }
        if (page < 1) page = 1;
        if (size < 1 || size > 200) size = 50;
        return messageMapper.selectSessionMessagePage(new Page<>(page, size), sessionId);
    }

    /** 取会话最近 N 条（按时间正序）— M4 context window 用 */
    public List<ChatMessage> loadRecentMessages(Long sessionId, int limit) {
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "sessionId 不能为空");
        }
        if (limit < 1 || limit > 200) limit = 50;
        return messageMapper.selectRecentSessionMessages(sessionId, limit);
    }

    // ===== 纯方法（无 DB） — 可单测 =====

    public String validateContent(String content) {
        if (content == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "消息内容不能为空");
        }
        String trimmed = content.strip();
        if (trimmed.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "消息内容不能为空");
        }
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                    "消息内容超长（> " + MAX_CONTENT_LENGTH + " 字符），请分段发送");
        }
        return trimmed;
    }
}
