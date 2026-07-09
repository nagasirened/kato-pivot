package com.kato.pro.langchain.domain.rag;

import com.kato.pro.langchain.domain.chat.ModelRouter;
import com.kato.pro.langchain.domain.chat.TaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 基于 M2.5 的 query 重写器（走 ModelRouter.SIMPLE_QUERY_REWRITE）。
 *
 * 失败时 fallback 到 NoOp（保留原 query），保证检索链路不中断。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmQueryRewriter implements QueryRewriter {

    private final ModelRouter modelRouter;

    @Override
    public String rewrite(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) return rawQuery == null ? "" : rawQuery;
        String systemPrompt = "你是 query 改写助手。把用户口语化/带错字的问句改写为清晰、无口语化、便于向量检索的中文短语，保留关键名词。" +
                "只输出改写后的短语，不要解释。";
        String rewritten = modelRouter.route(TaskType.SIMPLE_QUERY_REWRITE, systemPrompt, rawQuery).block();
        return (rewritten == null || rewritten.isBlank()) ? rawQuery : rewritten.trim();
    }
}
