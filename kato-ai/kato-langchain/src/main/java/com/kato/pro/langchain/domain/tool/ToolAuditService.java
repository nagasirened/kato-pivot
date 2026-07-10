package com.kato.pro.langchain.domain.tool;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.infrastructure.persistence.ToolCallAuditMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工具调用审计服务（A3 + 写类审核工作流）。
 *
 *   - enqueue         : WRITE 工具入队，状态 PENDING
 *   - executeApproved : admin approve 后真正执行工具；执行成功 → EXECUTED；异常 → FAILED
 *   - reject          : admin 拒绝；状态 REJECTED
 *   - recordReadInvocation : 读类也异步落审计（便于追溯）
 *
 * 配置 tool.audit-auto-approve=true 时 enqueue 直接执行（开发/测试用；生产 false）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolAuditService {

    private final ToolCallAuditMapper mapper;
    private final ToolRegistry registry;
    private final ToolInvoker invoker;
    private final ToolProperties properties;
    private final ObjectMapper objectMapper;

    @Transactional
    public Long enqueue(String toolName, String toolType, JsonNode args, ToolContext ctx) {
        ToolCallAudit audit = new ToolCallAudit();
        audit.setTenantId(ctx.getTenantId());
        audit.setToolName(toolName);
        audit.setToolType(toolType);
        audit.setArgsJson(args == null ? "{}" : args.toString());
        audit.setRequesterId(ctx.getUserId());
        audit.setChannel(ctx.getChannel() == null ? "user" : ctx.getChannel());
        audit.setStatus(AuditStatus.PENDING.name());
        audit.setTraceId(ctx.getTraceId());
        mapper.insert(audit);
        log.info("Tool audit enqueued: id={}, tool={}, tenant={}", audit.getId(), toolName, ctx.getTenantId());

        if (properties.isAuditAutoApprove()) {
            executeApproved(audit.getId());
        }
        return audit.getId();
    }

    @Transactional
    public ToolResult executeApproved(Long auditId) {
        ToolCallAudit a = mapper.selectById(auditId);
        if (a == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "audit 不存在");
        if (!AuditStatus.PENDING.name().equals(a.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                    "audit 状态非 PENDING: " + a.getStatus());
        }
        Tool tool = registry.require(a.getToolName());
        Long approverId = TenantContext.requireCurrent().userId();
        ToolContext ctx = ToolContext.builder()
                .tenantId(a.getTenantId())
                .userId(a.getRequesterId())
                .traceId(a.getTraceId())
                .channel(a.getChannel())
                .build();
        a.setApproverId(approverId);
        a.setApproveTime(LocalDateTime.now());
        a.setStatus(AuditStatus.APPROVED.name());

        try {
            JsonNode args = objectMapper.readTree(a.getArgsJson());
            ToolResult r = invoker.invoke(tool, args, ctx, properties.getDefaultTimeoutMs());
            if (r == null) {
                a.setStatus(AuditStatus.FAILED.name());
                a.setErrorMsg("null result");
            } else if (r.isSuccess()) {
                a.setStatus(AuditStatus.EXECUTED.name());
                a.setResultJson(objectMapper.writeValueAsString(r.getData()));
            } else {
                a.setStatus(AuditStatus.FAILED.name());
                a.setErrorMsg(r.getError());
            }
        } catch (Exception e) {
            log.error("Tool execute failed in audit approval: auditId={}", auditId, e);
            a.setStatus(AuditStatus.FAILED.name());
            a.setErrorMsg(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
        mapper.updateById(a);
        return ToolResult.ok(Map.of(
                "auditId", a.getId(),
                "status", a.getStatus(),
                "result", a.getResultJson() == null ? "" : a.getResultJson(),
                "error", a.getErrorMsg() == null ? "" : a.getErrorMsg()
        ));
    }

    @Transactional
    public ToolResult reject(Long auditId, Long approverId) {
        ToolCallAudit a = mapper.selectById(auditId);
        if (a == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "audit 不存在");
        if (!AuditStatus.PENDING.name().equals(a.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                    "audit 状态非 PENDING: " + a.getStatus());
        }
        a.setApproverId(approverId);
        a.setApproveTime(LocalDateTime.now());
        a.setStatus(AuditStatus.REJECTED.name());
        mapper.updateById(a);
        return ToolResult.ok(Map.of("auditId", a.getId(), "status", a.getStatus()));
    }

    @Transactional
    public void recordReadInvocation(String toolName, String toolType, JsonNode args,
                                     ToolResult result, ToolContext ctx) {
        ToolCallAudit audit = new ToolCallAudit();
        audit.setTenantId(ctx.getTenantId());
        audit.setToolName(toolName);
        audit.setToolType(toolType);
        audit.setArgsJson(args == null ? "{}" : args.toString());
        audit.setRequesterId(ctx.getUserId());
        audit.setChannel(ctx.getChannel() == null ? "user" : ctx.getChannel());
        audit.setTraceId(ctx.getTraceId());
        audit.setStatus(AuditStatus.EXECUTED.name());
        audit.setApproverId(ctx.getUserId());
        audit.setApproveTime(LocalDateTime.now());
        try {
            audit.setResultJson(objectMapper.writeValueAsString(result.getData()));
        } catch (Exception ignored) {}
        if (result != null && !result.isSuccess()) {
            audit.setStatus(AuditStatus.FAILED.name());
            audit.setErrorMsg(result.getError());
        }
        mapper.insert(audit);
    }

    public IPage<ToolCallAudit> page(Long tenantId, String status, String toolName,
                                     int page, int size) {
        return mapper.pageByStatus(tenantId, status, toolName, page, size);
    }
}
