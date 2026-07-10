package com.kato.pro.langchain.domain.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.common.security.AuthContext;
import com.kato.pro.langchain.common.security.AuthInfo;
import com.kato.pro.langchain.common.trace.TraceContext;
import com.kato.pro.langchain.infrastructure.persistence.OpAuditMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作审计写入服务（M11）。
 *
 * v1 同步写入（@Async 标记已加但 v1 默认 executor 不开启）；
 * 失败仅 log，不影响主业务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpAuditService {

    private final OpAuditMapper mapper;
    private final ObjectMapper objectMapper;

    /**
     * 记录一次操作审计。失败时 log 异常但不抛出。
     *
     * @param info       当前用户（来自 AuthContext）
     * @param action     CREATE/UPDATE/DELETE/TRIGGER/APPROVE/REJECT
     * @param resource   SYNC/KNOWLEDGE/TOOL/PROMPT/USER
     * @param resourceId 业务ID（可空）
     * @param requestUri HTTP 请求路径
     * @param method     HTTP 方法
     * @param httpStatus HTTP 状态码（成功时填 200；失败时由 caller 传）
     * @param args       原始入参
     * @param result     响应结果
     * @param durationMs 耗时
     */
    @Async
    public void record(AuthInfo info, String action, String resource, String resourceId,
                        String requestUri, String method, Integer httpStatus,
                        Object args, Object result, long durationMs) {
        try {
            OpAudit audit = new OpAudit();
            audit.setTenantId(info.tenantId());
            audit.setUserId(info.userId());
            audit.setUsername(info.username());
            audit.setAction(action);
            audit.setResource(resource);
            audit.setResourceId(resourceId);
            audit.setRequestUri(requestUri);
            audit.setHttpMethod(method);
            audit.setHttpStatus(httpStatus);
            audit.setArgsJson(toJson(args));
            audit.setResultJson(toJson(result));
            audit.setTraceId(TraceContext.current());
            audit.setDurationMs((int) Math.min(durationMs, Integer.MAX_VALUE));
            audit.setCreateTime(LocalDateTime.now());
            mapper.insert(audit);
        } catch (Exception e) {
            log.error("OpAudit write failed: user={} action={} resource={}",
                    info.username(), action, resource, e);
        }
    }

    /** 便捷：从 AuthContext 取当前用户 */
    public void recordFromContext(String action, String resource, String resourceId,
                                    String requestUri, String method, Integer httpStatus,
                                    Object args, Object result, long durationMs) {
        AuthInfo info = AuthContext.currentOrNull();
        if (info == null) {
            // 未登录：通常是系统内部调用（如启动期），跳过
            log.debug("OpAudit skip: no AuthContext (system call?) uri={}", requestUri);
            return;
        }
        record(info, action, resource, resourceId, requestUri, method, httpStatus, args, result, durationMs);
    }

    private String toJson(Object o) {
        if (o == null) return null;
        try {
            String s = objectMapper.writeValueAsString(o);
            // 截断超长（避免 TEXT 溢出）
            if (s.length() > 4000) s = s.substring(0, 4000) + "...";
            return s;
        } catch (JsonProcessingException e) {
            return "<unserializable: " + e.getOriginalMessage() + ">";
        }
    }
}
