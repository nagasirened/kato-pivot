package com.kato.pro.langchain.domain.memory;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.config.MemoryProperties;
import com.kato.pro.langchain.domain.chat.ChatMessage;
import com.kato.pro.langchain.domain.chat.ChatSession;
import com.kato.pro.langchain.domain.session.ChatMessageService;
import com.kato.pro.langchain.domain.session.ChatSessionService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 记忆管理门面（D5=A — 懒触发在 prepareContext 内）。
 *
 * 主流程：
 *   1. loadRecentMessages(sessionId) 取最近 N 条
 *   2. 估算 token 总数
 *   3. 不超 trigger → 返回原列表
 *   4. 超 trigger → 拆为 "old" + "tail"；old → HistorySummarizer；
 *                    若 tail 超 max → MemoryWindow.trim 进一步裁剪
 *   5. 组装返回：若旧摘要存在 → [SYSTEM(summary)] + tail
 */
@Slf4j
public class MemoryManager {

    /** 一次最多加载的消息数（M4 阶段保守值；v2 可根据 token 数动态调整） */
    public static final int DEFAULT_LOAD_LIMIT = 200;

    private final ChatSessionService sessionService;
    private final ChatMessageService messageService;
    private final TokenCountEstimator estimator;
    private final MemoryWindow window;
    private final HistorySummarizer summarizer;
    private final MemoryProperties properties;

    public MemoryManager(ChatSessionService sessionService,
                         ChatMessageService messageService,
                         TokenCountEstimator estimator,
                         MemoryWindow window,
                         HistorySummarizer summarizer,
                         MemoryProperties properties) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.estimator = estimator;
        this.window = window;
        this.summarizer = summarizer;
        this.properties = properties;
    }

    /**
     * 加载历史消息（可能含摘要 SYSTEM 消息）。
     */
    public List<ChatMessage> prepareContext(Long sessionId) {
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "sessionId 不能为空");
        }
        ChatSession session = sessionService.getSession(sessionId);

        List<ChatMessage> recent = messageService.loadRecentMessages(sessionId, DEFAULT_LOAD_LIMIT);
        if (recent.isEmpty()) {
            return List.of();
        }

        int trigger = properties.getSummary().getTriggerThresholdTokens();
        int total = estimator.estimateAll(extractContents(recent));

        if (total <= trigger) {
            // 不触发摘要；硬上限检查
            int max = effectiveMaxTokens();
            if (total > max) {
                return window.trim(recent, estimator, max);
            }
            return recent;
        }

        // 触发摘要：选最早 ~80% 作为 old，剩 20% 作为 tail
        int split = Math.max(1, (int) Math.floor(recent.size() * 0.8));
        List<ChatMessage> old = recent.subList(0, split);
        List<ChatMessage> tail = new ArrayList<>(recent.subList(split, recent.size()));

        boolean summarized = summarizer.summarize(session, old);
        // 重新读 session 拿最新 summary（CAS 成功才有意义）
        String currentSummary = summarized
                ? sessionService.getSession(sessionId).getSummary()
                : session.getSummary();

        int max = effectiveMaxTokens();
        // tail 可能仍超 max，再 trim
        List<ChatMessage> trimmedTail = (estimator.estimateAll(extractContents(tail)) > max)
                ? window.trim(tail, estimator, max)
                : tail;

        List<ChatMessage> result = new ArrayList<>();
        if (currentSummary != null && !currentSummary.isBlank()) {
            ChatMessage sys = new ChatMessage();
            sys.setRole(ChatMessage.Role.SYSTEM);
            sys.setContent("对话历史摘要：" + currentSummary);
            sys.setSessionId(sessionId);
            result.add(sys);
        }
        result.addAll(trimmedTail);
        return result;
    }

    /** 测试 / 管理用：强制刷新摘要（绕开 min-interval） */
    public void forceSummarize(Long sessionId) {
        ChatSession session = sessionService.getSession(sessionId);
        List<ChatMessage> recent = messageService.loadRecentMessages(sessionId, DEFAULT_LOAD_LIMIT);
        summarizer.forceSummarize(session, recent);
    }

    private int effectiveMaxTokens() {
        int max = properties.getWindow().getMaxTokens();
        double ratio = properties.getWindow().getReserveRatio();
        return (int) Math.floor(max * (1.0 - ratio));
    }

    private static List<String> extractContents(List<ChatMessage> msgs) {
        List<String> r = new ArrayList<>(msgs.size());
        for (ChatMessage m : msgs) {
            r.add(m.getContent() == null ? "" : m.getContent());
        }
        return r;
    }
}
