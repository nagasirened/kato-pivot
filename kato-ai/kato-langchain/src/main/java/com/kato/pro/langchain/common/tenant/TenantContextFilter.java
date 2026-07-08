package com.kato.pro.langchain.common.tenant;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 租户上下文过滤器：从 HTTP Header 中读取 tenantId/userId/channel，写入 TenantContext。
 *
 * Header 约定：
 *   X-Tenant-Id: 必填，租户 ID
 *   X-User-Id:   必填，当前用户 ID
 *   X-Channel:   可选，默认 "DEFAULT"（v2 用于区分网页/小程序/微信渠道）
 *
 * 顺序：必须在 TraceIdFilter 之后（这样日志中已有 traceId 可关联），但早于业务 Filter。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantContextFilter extends OncePerRequestFilter {

    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_CHANNEL = "X-Channel";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        // 放行健康检查与 OpenAPI 文档
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String tenantIdStr = request.getHeader(HEADER_TENANT_ID);
        String userIdStr = request.getHeader(HEADER_USER_ID);
        String channel = request.getHeader(HEADER_CHANNEL);

        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_MISMATCH, "请求头缺少 " + HEADER_TENANT_ID);
        }
        if (userIdStr == null || userIdStr.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_MISMATCH, "请求头缺少 " + HEADER_USER_ID);
        }

        Long tenantId;
        Long userId;
        try {
            tenantId = Long.parseLong(tenantIdStr);
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, HEADER_TENANT_ID + "/" + HEADER_USER_ID + " 必须是数字");
        }

        TenantInfo info = new TenantInfo(tenantId, userId, channel == null || channel.isBlank() ? "DEFAULT" : channel);
        try {
            TenantContext.set(info);
            log.debug("TenantContext set: {}", info);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/doc.html")
                || path.equals("/favicon.ico");
    }
}
