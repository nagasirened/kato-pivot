package com.kato.pro.langchain.domain.safety;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.common.trace.TraceContext;
import com.kato.pro.langchain.config.SafetyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 内容安全服务（D7=B — 暴露给 M8 ChatEngine 调用）。
 *
 *   - checkInput(userInput) : 入参检测（先于 LLM 调用）
 *   - checkOutput(reply)    : 出参检测（LLM 返回后）
 *
 * 全局开关：safety.enabled=false → 直接 pass。
 */
@Slf4j
@Service
public class ContentSafetyService {

    private final SafetyProperties properties;
    private final ContentSafetyFilter composite;

    private final AtomicLong fallbackCount = new AtomicLong();
    private final AtomicLong rejectCount = new AtomicLong();

    public ContentSafetyService(SafetyProperties properties, ContentSafetyFilter composite) {
        this.properties = properties;
        this.composite = composite;
    }

    public SafetyResult checkInput(String text) {
        if (!properties.isEnabled()) return SafetyResult.pass();
        return doCheck(text, SafetyContext.Direction.INPUT);
    }

    public SafetyResult checkOutput(String text) {
        if (!properties.isEnabled()) return SafetyResult.pass();
        return doCheck(text, SafetyContext.Direction.OUTPUT);
    }

    /** reject 时抛业务异常（带 sanitized 文本作为回退消息） */
    public void assertPassedOrThrow(SafetyResult r) {
        if (r == null || r.isPassed()) return;
        rejectCount.incrementAndGet();
        String msg = "内容安全检测未通过";
        if (r.getSanitizedText() != null && !r.getSanitizedText().isBlank()) {
            msg = msg + "（建议改写：" + r.getSanitizedText() + "）";
        }
        throw new BusinessException(ErrorCode.PARAM_INVALID, msg);
    }

    private SafetyResult doCheck(String text, SafetyContext.Direction dir) {
        SafetyContext ctx = SafetyContext.builder()
                .direction(dir)
                .tenantId(TenantContext.currentOrNull() != null ? TenantContext.currentTenantId() : null)
                .traceId(TraceContext.current())
                .build();
        SafetyResult r = composite.check(text, ctx);
        if (r.isFallback()) fallbackCount.incrementAndGet();
        if (!r.isPassed()) rejectCount.incrementAndGet();
        log.debug("Safety check dir={} passed={} fallback={} hits={}",
                dir, r.isPassed(), r.isFallback(), r.getHitWords());
        return r;
    }

    public long getFallbackCount() { return fallbackCount.get(); }
    public long getRejectCount() { return rejectCount.get(); }
}
