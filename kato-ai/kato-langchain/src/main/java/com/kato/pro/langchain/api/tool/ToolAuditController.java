package com.kato.pro.langchain.api.tool;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kato.pro.langchain.api.chat.dto.PageResult;
import com.kato.pro.langchain.api.tool.dto.ApproveRequest;
import com.kato.pro.langchain.api.tool.dto.AuditListVO;
import com.kato.pro.langchain.api.tool.dto.AuditStatsVO;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.result.Result;
import com.kato.pro.langchain.common.security.RequireRole;
import com.kato.pro.langchain.common.security.Role;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.domain.audit.OpAuditLog;
import com.kato.pro.langchain.domain.tool.ToolAuditService;
import com.kato.pro.langchain.domain.tool.ToolAuditStatsService;
import com.kato.pro.langchain.domain.tool.ToolCallAudit;
import com.kato.pro.langchain.domain.tool.ToolDispatcher;
import com.kato.pro.langchain.domain.tool.ToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工具审核 REST API（admin + M11 RBAC）。
 *
 *   GET  /api/v1/admin/tool/audit?status=&toolName=&page=&size=    审核列表（ADMIN/OPERATOR）
 *   POST /api/v1/admin/tool/audit/{id}/approve                     审核通过（ADMIN）
 *   POST /api/v1/admin/tool/audit/{id}/reject                      审核拒绝（ADMIN）
 *   GET  /api/v1/admin/tool/audit/stats?days=7                     维度统计（ADMIN/OPERATOR）
 */
@RestController
@RequestMapping("/api/v1/admin/tool/audit")
@RequiredArgsConstructor
public class ToolAuditController {

    private final ToolAuditService auditService;
    private final ToolAuditStatsService statsService;
    private final ToolDispatcher dispatcher;

    @GetMapping
    @RequireRole({Role.ADMIN, Role.OPERATOR})
    public Result<PageResult<AuditListVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String toolName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long tenantId = TenantContext.requireCurrent().tenantId();
        IPage<ToolCallAudit> p = auditService.page(tenantId, status, toolName, page, size);
        return Result.ok(PageResult.of(p, AuditListVO::from));
    }

    @PostMapping("/{id}/approve")
    @RequireRole(Role.ADMIN)
    @OpAuditLog(action = "APPROVE", resource = "TOOL")
    public Result<ToolResult> approve(@PathVariable Long id, @RequestBody(required = false) ApproveRequest req) {
        TenantContext.requireCurrent();
        return Result.ok(dispatcher.executeApproved(id));
    }

    @PostMapping("/{id}/reject")
    @RequireRole(Role.ADMIN)
    @OpAuditLog(action = "REJECT", resource = "TOOL")
    public Result<ToolResult> reject(@PathVariable Long id, @RequestBody(required = false) ApproveRequest req) {
        if (req == null) throw new BusinessException(ErrorCode.PARAM_INVALID, "body 不能为空");
        Long approverId = TenantContext.requireCurrent().userId();
        return Result.ok(auditService.reject(id, approverId));
    }

    @GetMapping("/stats")
    @RequireRole({Role.ADMIN, Role.OPERATOR})
    public Result<AuditStatsVO> stats(@RequestParam(defaultValue = "7") int days) {
        if (days < 1 || days > 90) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "days 必须在 1-90");
        }
        return Result.ok(statsService.stats(days));
    }
}
