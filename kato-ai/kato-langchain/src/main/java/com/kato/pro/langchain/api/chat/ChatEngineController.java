package com.kato.pro.langchain.api.chat;

import com.kato.pro.langchain.api.chat.dto.ChatResponseVO;
import com.kato.pro.langchain.api.chat.dto.SendMessageRequest;
import com.kato.pro.langchain.api.chat.dto.SseEvent;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.result.Result;
import com.kato.pro.langchain.config.ChatEngineProperties;
import com.kato.pro.langchain.domain.chat.ChatApplicationService;
import com.kato.pro.resilience.annotation.KeyBy;
import com.kato.pro.resilience.annotation.RateLimited;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;



/**
 * ChatEngine REST API（spec §6 步骤 10 双协议）。
 *
 *   POST /api/v1/chat/sessions/{id}/messages        非流式：一次性 JSON 回复
 *   POST /api/v1/chat/sessions/{id}/messages/stream 流式：SseEmitter 分片推送
 *
 * 限流：每租户 20 QPS（spec §6 M12），超出返回 HTTP 429。
 */
@Slf4j
@Tag(name = "Chat-Engine", description = "对话引擎：发送消息（非流式/流式），含限流")
@RestController
@RequestMapping("/api/v1/chat/sessions/{id}")
@RequiredArgsConstructor
public class ChatEngineController {

    private final ChatApplicationService chatService;
    private final ChatEngineProperties properties;

    @Operation(operationId = "SendMessage", summary = "发送消息（非流式）", description = "一次性 JSON 回复；每租户限流 20 QPS")
    @PostMapping("/messages")
    @RateLimited(key = "chat.send", keyBy = KeyBy.TENANT, permitsPerSecond = 20)
    public Result<ChatResponseVO> send(@PathVariable Long id, @Valid @RequestBody SendMessageRequest req) {
        if (req == null || req.getContent() == null || req.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "content 不能为空");
        }
        String reply = chatService.sendMessage(id, req.getContent()).block();
        return Result.ok(ChatResponseVO.builder()
                .sessionId(id)
                .reply(reply)
                .build());
    }

    @Operation(operationId = "StreamMessage", summary = "发送消息（流式）", description = "SSE 流式分片推送；每租户限流 10 QPS")
    @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimited(key = "chat.stream", keyBy = KeyBy.TENANT, permitsPerSecond = 10)
    public SseEmitter stream(@PathVariable Long id, @RequestBody SendMessageRequest req) {
        if (req == null || req.getContent() == null || req.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "content 不能为空");
        }
        SseEmitter emitter = new SseEmitter(properties.getSseTimeoutMs());

        chatService.sendMessage(id, req.getContent())
                .doOnNext(reply -> emit(emitter, SseEvent.chunk(reply)))
                .doOnError(err -> emit(emitter, SseEvent.error(err.getMessage())))
                .doOnSuccess(reply -> emit(emitter, SseEvent.end(reply)))
                .doFinally(s -> emitter.complete())
                .subscribe();

        return emitter;
    }

    private void emit(SseEmitter emitter, SseEvent event) {
        try {
            emitter.send(SseEmitter.event().name(event.type()).data(event.data()));
        } catch (IOException e) {
            log.warn("SSE send failed: {}", e.getMessage());
            emitter.completeWithError(e);
        }
    }
}
