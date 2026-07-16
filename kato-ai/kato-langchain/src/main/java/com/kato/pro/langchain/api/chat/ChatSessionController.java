package com.kato.pro.langchain.api.chat;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kato.pro.langchain.api.chat.dto.CreateSessionRequest;
import com.kato.pro.langchain.api.chat.dto.MessageVO;
import com.kato.pro.langchain.api.chat.dto.PageResult;
import com.kato.pro.langchain.api.chat.dto.SessionVO;
import com.kato.pro.langchain.common.result.Result;
import com.kato.pro.langchain.domain.chat.ChatMessage;
import com.kato.pro.langchain.domain.chat.ChatSession;
import com.kato.pro.langchain.domain.session.ChatMessageService;
import com.kato.pro.langchain.domain.session.ChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会话 REST API。所有 endpoint 都自动受 M1 的 TenantContextFilter 保护（要求 X-Tenant-Id/X-User-Id header）。
 *
 * 注意：M3 阶段不暴露"发送消息"接口（POST /sessions/{sid}/messages）— 那是 M8 ChatEngine 的事。
 */
@Slf4j
@Tag(name = "Chat-Session", description = "会话管理：创建、查询、归档、历史消息")
@RestController
@RequestMapping("/api/v1/chat/sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService sessionService;
    private final ChatMessageService messageService;

    @Operation(operationId = "CreateSession", summary = "创建会话",
            description = "新建一个客服会话，返回 sessionId")
    @PostMapping
    public Result<SessionVO> create(@RequestBody(required = false) CreateSessionRequest req) {
        String title = req == null ? null : req.getTitle();
        ChatSession s = sessionService.createSession(title);
        return Result.ok(SessionVO.from(s));
    }

    @Operation(operationId = "ListSessions", summary = "查询会话列表",
            description = "分页查询当前租户的会话列表")
    @GetMapping
    public Result<PageResult<SessionVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        IPage<ChatSession> p = sessionService.listUserActiveSessions(page, size);
        return Result.ok(PageResult.of(p, SessionVO::from));
    }

    @Operation(operationId = "GetSession", summary = "会话详情",
            description = "按 sid 查询会话元数据")
    @GetMapping("/{sid}")
    public Result<SessionVO> get(@PathVariable Long sid) {
        ChatSession s = sessionService.getSession(sid);
        return Result.ok(SessionVO.from(s));
    }

    @Operation(operationId = "ArchiveSession", summary = "归档会话",
            description = "将会话标记为已归档")
    @PatchMapping("/{sid}/archive")
    public Result<Boolean> archive(@PathVariable Long sid) {
        boolean ok = sessionService.archiveSession(sid);
        return Result.ok(ok);
    }

    @Operation(operationId = "ListMessages", summary = "会话历史消息",
            description = "分页查询会话历史消息")
    @GetMapping("/{sid}/messages")
    public Result<PageResult<MessageVO>> listMessages(
            @PathVariable Long sid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        // 先校验 session 属于当前用户（getSession 会抛错）
        sessionService.getSession(sid);
        IPage<ChatMessage> p = messageService.listSessionMessages(sid, page, size);
        return Result.ok(PageResult.of(p, MessageVO::from));
    }
}
