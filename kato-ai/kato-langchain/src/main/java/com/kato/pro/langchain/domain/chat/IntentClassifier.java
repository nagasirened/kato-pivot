package com.kato.pro.langchain.domain.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * 意图分类器（spec §6 步骤 5）— 调 M2.5 把用户输入三态路由。
 *
 * 输入：userInput 原文
 * 输出：Intent 枚举
 *
 * 失败兜底：
 *   - 模型超时 / 异常 → 默认 RAG_ONLY（保守：先查知识库，比闲聊更稳）
 *   - 输出不是合法枚举 → 默认 RAG_ONLY
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentClassifier {

    private final ModelRouter modelRouter;

    private static final String SYSTEM_PROMPT = """
            你是一个意图分类器。把用户输入分为以下三类之一：
              TOOL_CALL  — 需要调用工具（查订单、退款、改地址等具体操作）
              RAG_ONLY   — 知识库问答（产品咨询、文档查询、政策说明）
              CHITCHAT   — 闲聊（打招呼、问候、感谢、与业务无关）
            只输出枚举名，不要输出其它内容。
            """;

    private static final Set<String> VALID = Set.of("TOOL_CALL", "RAG_ONLY", "CHITCHAT");

    public Mono<Intent> classify(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return Mono.just(Intent.CHITCHAT);
        }
        return modelRouter.route(TaskType.SIMPLE_CLASSIFICATION, SYSTEM_PROMPT, userInput)
                .map(this::parse)
                .onErrorResume(e -> {
                    log.warn("Intent classify failed, defaulting to RAG_ONLY: {}", e.getMessage());
                    return Mono.just(Intent.RAG_ONLY);
                });
    }

    private Intent parse(String raw) {
        if (raw == null) return Intent.RAG_ONLY;
        String s = raw.trim().toUpperCase();
        for (String v : VALID) {
            if (s.contains(v)) {
                return Intent.valueOf(v);
            }
        }
        log.warn("Unrecognized intent output: '{}', defaulting to RAG_ONLY", raw);
        return Intent.RAG_ONLY;
    }
}
