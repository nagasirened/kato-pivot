package com.kato.pro.langchain.api.admin.sync;

import com.kato.pro.langchain.api.admin.sync.dto.SyncRunVO;
import com.kato.pro.langchain.api.admin.sync.dto.SyncStatusVO;
import com.kato.pro.langchain.api.admin.sync.dto.TriggerResponse;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.result.Result;
import com.kato.pro.langchain.common.security.RequireRole;
import com.kato.pro.langchain.common.security.Role;
import com.kato.pro.langchain.domain.audit.OpAuditLog;
import com.kato.pro.langchain.domain.sync.SyncApplicationService;
import com.kato.pro.langchain.domain.sync.SyncRunRecord;
import com.kato.pro.resilience.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;



/**
 * 知识库同步 admin REST API（spec §6 M9 + M11 RBAC + M12 熔断）。
 *
 *   POST /api/v1/admin/sync/{adapterName}/trigger  手动触发（ADMIN）— CircuitBreaker 保护
 *   GET  /api/v1/admin/sync/status                状态概览（ADMIN/OPERATOR）
 *   GET  /api/v1/admin/sync/runs?adapter=&limit=  指定 adapter 的最近运行记录（ADMIN/OPERATOR）
 */
@Tag(name = "Knowledge-Sync", description = "知识库同步 admin 接口（触发、状态、最近运行）")
@Validated
@RestController
@RequestMapping("/api/v1/admin/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncApplicationService syncService;

    @Operation(operationId = "TriggerSync", summary = "触发同步", description = "手动触发指定 adapter 的全量同步；带熔断")
    @PostMapping("/{adapterName}/trigger")
    @RequireRole(Role.ADMIN)
    @OpAuditLog(action = "TRIGGER", resource = "SYNC")
    @CircuitBreaker(name = "sync.trigger")
    public Result<TriggerResponse> trigger(@PathVariable String adapterName) {
        SyncRunRecord run = syncService.runOne(adapterName);
        return Result.ok(TriggerResponse.builder()
                .adapterName(run.getAdapterName())
                .runId(run.getId())
                .status(run.getStatus())
                .successCount(run.getSuccessCount())
                .failCount(run.getFailCount())
                .errorMsg(run.getErrorMsg())
                .build());
    }

    @Operation(operationId = "SyncStatus", summary = "同步状态", description = "查询所有 adapter 的最新同步状态")
    @GetMapping("/status")
    @RequireRole({Role.ADMIN, Role.OPERATOR})
    public Result<List<SyncStatusVO>> status() {
        List<Map<String, Object>> raw = syncService.statusOverview();
        List<SyncStatusVO> result = raw.stream().map(e -> {
            Map<String, Object> lastRun = toLastRunMap((Map<String, Object>) e.get("lastRun"));
            return SyncStatusVO.builder()
                    .adapterName((String) e.get("adapterName"))
                    .description((String) e.get("description"))
                    .sourceType((String) e.get("sourceType"))
                    .lastRun(lastRun)
                    .build();
        }).toList();
        return Result.ok(result);
    }

    @Operation(operationId = "SyncRuns", summary = "同步运行记录", description = "查询指定 adapter 的最近 N 条运行")
    @GetMapping("/runs")
    @RequireRole({Role.ADMIN, Role.OPERATOR})
    public Result<List<SyncRunVO>> runs(@RequestParam @NotBlank String adapter,
                                          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        if (adapter == null || adapter.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "adapter 不能为空");
        }
        if (limit < 1 || limit > 100) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "limit 必须在 1-100");
        }
        List<SyncRunRecord> records = syncService.recentRuns(adapter, limit);
        return Result.ok(records.stream().map(SyncRunVO::from).toList());
    }

    private Map<String, Object> toLastRunMap(Map<String, Object> lastRun) {
        if (lastRun == null) return null;
        Map<String, Object> m = new HashMap<>();
        m.put("id", lastRun.get("id"));
        m.put("startTime", lastRun.get("startTime"));
        m.put("endTime", lastRun.get("endTime"));
        m.put("successCount", lastRun.get("successCount"));
        m.put("failCount", lastRun.get("failCount"));
        m.put("status", lastRun.get("status"));
        m.put("errorMsg", lastRun.get("errorMsg"));
        return m;
    }
}
