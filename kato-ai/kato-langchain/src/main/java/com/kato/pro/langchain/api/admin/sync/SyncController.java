package com.kato.pro.langchain.api.admin.sync;

import com.kato.pro.langchain.api.admin.sync.dto.SyncRunVO;
import com.kato.pro.langchain.api.admin.sync.dto.SyncStatusVO;
import com.kato.pro.langchain.api.admin.sync.dto.TriggerResponse;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.result.Result;
import com.kato.pro.langchain.domain.sync.SyncApplicationService;
import com.kato.pro.langchain.domain.sync.SyncRunRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库同步 admin REST API（spec §6 M9）。
 *
 *   POST /api/v1/admin/sync/{adapterName}/trigger  手动触发
 *   GET  /api/v1/admin/sync/status                状态概览（所有 adapter）
 *   GET  /api/v1/admin/sync/runs?adapter=&limit=  指定 adapter 的最近运行记录
 *
 * v1：admin 鉴权暂未接入（TODO M11）；生产应加 @PreAuthorize 限 admin 角色。
 * v1：runs 接口必须指定 adapter（跨 adapter 查询在 v2 加入）。
 */
@RestController
@RequestMapping("/api/v1/admin/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncApplicationService syncService;

    @PostMapping("/{adapterName}/trigger")
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

    @GetMapping("/status")
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

    @GetMapping("/runs")
    public Result<List<SyncRunVO>> runs(@RequestParam String adapter,
                                          @RequestParam(defaultValue = "20") int limit) {
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
