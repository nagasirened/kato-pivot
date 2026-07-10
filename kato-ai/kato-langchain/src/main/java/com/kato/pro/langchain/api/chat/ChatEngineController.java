package com.kato.pro.langchain.api.chat;

import com.kato.pro.langchain.api.chat.dto.ChatResponseVO;
import com.kato.pro.langchain.api.chat.dto.SendMessageRequest;
import com.kato.pro.langchain.api.chat.dto.SseEvent;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.result.Result;
import com.kato.pro.langchain.config.ChatEngineProperties;
import com.kato.pro.langchain.domain.chat.ChatApplicationService;
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

/**
 * ChatEngine REST API（spec §6 步骤 10 双协议）。
 *
 *   POST /api/v1/chat/sessions/{id}/messages        非流式：一次性 JSON 回复
 *   POST /api/v1/chat/sessions/{id}/messages/stream 流式：SseEmitter 分片推送
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat/sessions/{id}")
@RequiredArgsConstructor
public class ChatEngineController {

    private final ChatApplicationService chatService;
    private final ChatEngineProperties properties;

    @PostMapping("/messages")
    public Result<ChatResponseVO> send(@PathVariable Long id, @RequestBody SendMessageRequest req) {
        if (req == null || req.getContent() == null || req.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "content 不能为空");
        }
        String reply = chatService.sendMessage(id, req.getContent()).block();
        return Result.ok(ChatResponseVO.builder()
                .sessionId(id)
                .reply(reply)
                .build());
    }

    @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
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
