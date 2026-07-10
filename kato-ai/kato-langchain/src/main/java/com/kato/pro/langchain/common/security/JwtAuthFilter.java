package com.kato.pro.langchain.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 鉴权过滤器（spec §6 M11）。
 *
 * 注册：WebConfig.filterRegistration(jwtAuthFilter) — 显式控制 URL patterns 与顺序。
 * 顺序：TraceId → TenantContext → Auth（最后执行）
 *
 * 行为：
 *   - 解析 Authorization: Bearer xxx → JwtTokenService.parse → AuthContext.set
 *   - 失败 → 401 JSON（绕过 GlobalExceptionHandler 的 Result 包装）
 *   - 成功 → chain.doFilter；finally → AuthContext.clear()
 *
 * 跳过：v1 仅 /api/v1/auth/login、/api/v1/internal/health、/v3/api-docs、/doc.html 等。
 * 业务层 @RequireRole 决定哪些端点真的需要鉴权。
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenService tokenService;
    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    /** 跳过鉴权的路径前缀（v1 白名单；v2 走配置化） */
    private static final String[] SKIP_PREFIXES = {
            "/api/v1/auth/",
            "/api/v1/internal/health",
            "/v3/api-docs",
            "/doc.html",
            "/swagger-ui",
            "/actuator",
            "/favicon.ico"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String prefix : SKIP_PREFIXES) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(properties.getHeader());
        String token = tokenService.extractFromHeader(header);
        try {
            if (token == null || token.isBlank()) {
                // 无 token：放行（业务层 @RequireRole 决定是否要鉴权）
                chain.doFilter(request, response);
                return;
            }
            AuthInfo info = tokenService.parse(token);
            AuthContext.set(info);
            try {
                chain.doFilter(request, response);
            } finally {
                AuthContext.clear();
            }
        } catch (BusinessException e) {
            log.warn("JWT auth failed: code={} msg={}", e.getErrorCode().getCode(), e.getMessage());
            writeUnauthorized(response, e.getErrorCode().getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("JWT auth unexpected error", e);
            writeUnauthorized(response, ErrorCode.UNAUTHORIZED.getCode(), "鉴权失败");
        }
    }

    private void writeUnauthorized(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
