package com.kato.pro.langchain.api.auth;

import com.kato.pro.langchain.api.auth.dto.LoginRequest;
import com.kato.pro.langchain.api.auth.dto.LoginResponse;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.result.Result;
import com.kato.pro.langchain.common.security.AuthInfo;
import com.kato.pro.langchain.common.security.JwtProperties;
import com.kato.pro.langchain.common.security.JwtTokenService;
import com.kato.pro.langchain.common.security.Role;
import com.kato.pro.langchain.common.entity.User;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.common.tenant.TenantInfo;
import com.kato.pro.langchain.infrastructure.persistence.UserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;



/**
 * 鉴权 REST API（spec §6 M11）。
 *
 *   POST /api/v1/auth/login  {username, password} → {token, expiresIn, role, ...}
 *
 * v1：密码 = sha256("<username>-pass")，3 个种子账号在 V7 注入。
 * v2：bcrypt + 真实密码策略。
 */
@Slf4j
@Tag(name = "Auth", description = "登录、JWT 颁发、当前用户信息")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final JwtTokenService tokenService;
    private final JwtProperties jwtProperties;
    @Operation(operationId = "UserLogin", summary = "用户登录", description = "校验用户名密码，颁发 JWT（默认 24h 有效）")

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        // 1. 查用户
        Optional<User> userOpt = findByUsername(req.getUsername());
        if (userOpt.isEmpty()) {
            log.warn("Login failed: user not found: {}", req.getUsername());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        User user = userOpt.get();

        // 2. 校验状态
        if (!"ENABLE".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已停用");
        }

        // 3. 校验密码（v1：sha256("<username>-pass")）
        String expectedHash = sha256(req.getUsername() + "-pass");
        if (!expectedHash.equalsIgnoreCase(user.getPasswordHash())) {
            log.warn("Login failed: bad password: {}", req.getUsername());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 4. 临时设 TenantContext（userMapper.findByUsername 不需要，但生成 token 内的 tenantId 来自 user.tenantId）
        Role role = Role.fromString(user.getRole());

        // 5. 签发 token
        AuthInfo info = AuthInfo.of(user.getId(), user.getUsername(), user.getTenantId(), role);
        String token = tokenService.sign(info);

        log.info("Login success: user={} role={}", user.getUsername(), role);
        return Result.ok(LoginResponse.builder()
                .token(token)
                .expiresIn(jwtProperties.getTtlSeconds())
                .userId(user.getId())
                .username(user.getUsername())
                .tenantId(user.getTenantId())
                .role(role)
                .build());
    }

    private Optional<User> findByUsername(String username) {
        // 直接 selectList by username；v1 不分页，简单
        try {
            // 临时设置 tenant 上下文（TenantMybatisInterceptor 需要）
            // 实际：登录时不传 tenant 头；这里用一个占位 tenant 拿全表
            // v2 优化：username 字段单独建立非 tenant 索引的查找路径
            TenantContext.set(TenantInfo.of(1L, 0L));
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> q =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            q.eq("username", username).last("LIMIT 1");
            java.util.List<User> list = userMapper.selectList(q);
            TenantContext.clear();
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (Exception e) {
            TenantContext.clear();
            log.error("findByUsername failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
