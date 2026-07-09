package com.kato.pro.langchain.domain.memory;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.config.MemoryProperties;
import com.kato.pro.langchain.domain.chat.ChatMessage;
import com.kato.pro.langchain.domain.chat.ChatSession;
import com.kato.pro.langchain.domain.chat.ModelRouter;
import com.kato.pro.langchain.domain.chat.TaskType;
import com.kato.pro.langchain.domain.session.ChatSessionService;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 历史摘要器（C2 — 超窗用 M2.5 摘要）。
 *
 * 核心逻辑：
 *   - 输入：旧摘要（可能为空）+ 超出窗口的消息列表
 *   - 调 M2.5（走 ModelRouter.SIMPLE_SUMMARIZATION 任务通道）
 *   - 与旧摘要"增量合并"：拼接成新摘要
 *   - 通过 ChatSessionService.updateSummary 做 CAS 写回
 *
 * 限频（D3=A）：minIntervalSeconds — 同一 sessionId 距上次摘要 ≥ 该值才允许再次摘要
 *       （用 ConcurrentHashMap 缓存上次摘要时间；v2 改 Redisson）
 */
@Slf4j
// 注：不加 @Service，由 MemoryConfig 装配（依赖多个组件）
public class HistorySummarizer {

    private final ModelRouter modelRouter;
    private final ChatSessionService sessionService;
    private final TokenCountEstimator tokenEstimator;
    private final MemoryProperties properties;
    private final Map<Long, LocalDateTime> lastSummarizedAt = new ConcurrentHashMap<>();

    public HistorySummarizer(ModelRouter modelRouter,
                             ChatSessionService sessionService,
                             TokenCountEstimator tokenEstimator,
                             MemoryProperties properties) {
        this.modelRouter = modelRouter;
        this.sessionService = sessionService;
        this.tokenEstimator = tokenEstimator;
        this.properties = properties;
    }

    /**
     * 尝试生成并写回摘要。
     *
     * @param session    当前会话
     * @param toSummarize 需要被摘要的消息（旧→新）
     * @return true 表示成功写回；false 表示被限频跳过 / CAS 失败 / 无内容
     */
    public boolean summarize(ChatSession session, List<ChatMessage> toSummarize) {
        if (session == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "session 不能为空");
        }
        if (toSummarize == null || toSummarize.isEmpty()) {
            return false;
        }

        Long sid = session.getId();
        // 限频检查
        if (!canSummarize(sid)) {
            log.debug("Skip summarize: session={} within min-interval", sid);
            return false;
        }

        String oldSummary = session.getSummary() == null ? "" : session.getSummary();
        String fresh = callModelForSummary(toSummarize);
        String merged = merge(oldSummary, fresh);

        boolean ok = sessionService.updateSummary(sid, merged, session.getSummaryVersion());
        if (ok) {
            lastSummarizedAt.put(sid, LocalDateTime.now());
            log.info("Summarized session={} chars={} version={}->{}",
                    sid, merged.length(), session.getSummaryVersion(), session.getSummaryVersion() + 1);
        } else {
            log.debug("CAS updateSummary lost race for session={}", sid);
        }
        return ok;
    }

    /** 强制刷新（绕开限频）。仅测试/管理接口使用。 */
    public boolean forceSummarize(ChatSession session, List<ChatMessage> toSummarize) {
        lastSummarizedAt.remove(session.getId());
        return summarize(session, toSummarize);
    }

    private boolean canSummarize(Long sessionId) {
        LocalDateTime last = lastSummarizedAt.get(sessionId);
        if (last == null) return true;
        long minSeconds = properties.getSummary().getMinIntervalSeconds();
        return Duration.between(last, LocalDateTime.now()).getSeconds() >= minSeconds;
    }

    private String callModelForSummary(List<ChatMessage> msgs) {
        StringBuilder sb = new StringBuilder();
        sb.append("请将以下对话历史压缩为一段简短摘要（中文，100~200 字），保留关键事实和未解决问题：\n\n");
        for (ChatMessage m : msgs) {
            String role = m.getRole() == null ? "?" : m.getRole().name();
            sb.append("[").append(role).append("] ").append(m.getContent() == null ? "" : m.getContent()).append("\n");
        }
        String response = modelRouter
                .route(TaskType.SIMPLE_SUMMARIZATION, "你是对话摘要助手。", sb.toString())
                .block();
        return Objects.requireNonNullElse(response, "");
    }

    /** 增量合并（D4=B）：旧摘要 + 新段。空摘要视为"无前缀"。 */
    public String merge(String oldSummary, String fresh) {
        String o = oldSummary == null ? "" : oldSummary.trim();
        String f = fresh == null ? "" : fresh.trim();
        if (o.isEmpty()) return f;
        if (f.isEmpty()) return o;
        return o + "\n\n[续]\n" + f;
    }
}
