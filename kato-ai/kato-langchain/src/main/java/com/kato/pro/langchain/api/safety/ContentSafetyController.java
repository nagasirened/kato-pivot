package com.kato.pro.langchain.api.safety;

import com.kato.pro.langchain.api.safety.dto.SafetyCheckRequest;
import com.kato.pro.langchain.api.safety.dto.SafetyResultVO;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.result.Result;
import com.kato.pro.langchain.domain.safety.ContentSafetyService;
import com.kato.pro.langchain.domain.safety.SafetyContext;
import com.kato.pro.langchain.domain.safety.SafetyResult;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内容安全 REST API：
 *   POST /api/v1/safety/check   入参/出参检测（按 direction 路由）
 *   GET  /api/v1/safety/stats   监控指标（fallback + reject 计数）
 *
 * 命中违规时 controller 不会抛异常 — 由调用方根据 passed 决定下一步（M8 走 assertPassedOrThrow）。
 */
@RestController
@RequestMapping("/api/v1/safety")
@RequiredArgsConstructor
public class ContentSafetyController {

    private final ContentSafetyService service;

    @PostMapping("/check")
    public Result<SafetyResultVO> check(@RequestBody SafetyCheckRequest req) {
        if (req == null || req.getText() == null || req.getText().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "text 不能为空");
        }
        SafetyContext.Direction dir = parseDirection(req.getDirection());
        SafetyResult r = (dir == SafetyContext.Direction.OUTPUT)
                ? service.checkOutput(req.getText())
                : service.checkInput(req.getText());
        return Result.ok(SafetyResultVO.from(r));
    }

    @GetMapping("/stats")
    public Result<StatsVO> stats() {
        return Result.ok(StatsVO.builder()
                .fallbackCount(service.getFallbackCount())
                .rejectCount(service.getRejectCount())
                .build());
    }

    private SafetyContext.Direction parseDirection(String dir) {
        if (dir == null || dir.isBlank() || "INPUT".equalsIgnoreCase(dir)) {
            return SafetyContext.Direction.INPUT;
        }
        if ("OUTPUT".equalsIgnoreCase(dir)) {
            return SafetyContext.Direction.OUTPUT;
        }
        throw new BusinessException(ErrorCode.PARAM_INVALID, "direction 必须是 INPUT 或 OUTPUT");
    }

    @Value
    @Builder
    public static class StatsVO {
        long fallbackCount;
        long rejectCount;
    }
}
