package com.kato.pro.langchain.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TraceId 注入过滤器：必须早于 TenantContextFilter（Ordered.HIGHEST_PRECEDENCE + 10），以保证
 * TenantContextFilter 抛错时日志已带 traceId，便于排查。
 *
 * 行为：
 *   - 若请求头已带 X-Trace-Id，沿用（便于上游 / 网关串联）
 *   - 否则生成 UUID 写入 MDC
 *   - 响应 Header 同步回写 X-Trace-Id
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String incoming = request.getHeader(HEADER_TRACE_ID);
        String traceId = (incoming != null && !incoming.isBlank()) ? incoming : TraceContext.generate();
        TraceContext.set(traceId);
        response.setHeader(HEADER_TRACE_ID, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }
}
