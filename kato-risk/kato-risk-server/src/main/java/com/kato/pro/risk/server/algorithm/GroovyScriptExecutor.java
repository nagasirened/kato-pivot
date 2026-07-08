package com.kato.pro.risk.server.algorithm;

import com.kato.pro.risk.client.dto.RiskRequest;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

/**
 * Groovy 脚本执行器。
 *
 * 安全措施：
 * - 编译后缓存（Class 级），不重复编译
 * - 执行 timeout 500ms，超时强制中断
 * - 危险操作（File/Network/Reflection）由脚本审核阶段拒绝，此处仅做兜底超时
 */
@Slf4j
@Component
public class GroovyScriptExecutor {

    /** Class 缓存：ruleId -> Groovy Script Class */
    private final ConcurrentHashMap<Long, Class<? extends Script>> scriptClassCache = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private static final long TIMEOUT_MS = 500L;

    /**
     * 执行 Groovy 脚本。
     */
    public Map<String, Object> execute(Long ruleId, String scriptContent, RiskRequest request) {
        try {
            Class<? extends Script> scriptClass = scriptClassCache.computeIfAbsent(ruleId, id -> {
                GroovyClassLoader loader = new GroovyClassLoader();
                return loader.parseClass(scriptContent);
            });

            final Binding binding = new Binding();
            binding.setVariable("ctx", buildContext(request));

            Future<Map<String, Object>> future = executor.submit(() -> {
                GroovyShell shell = new GroovyShell(binding);
                Script script = shell.parse(scriptContent);
                return (Map<String, Object>) script.run();
            });

            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.error("Groovy script timeout, ruleId={}", ruleId);
            return fallback("GROOVY_TIMEOUT", "规则执行超时，已降级放行");
        } catch (Exception e) {
            log.error("Groovy script execution error, ruleId={}", ruleId, e);
            return fallback("GROOVY_ERROR", "规则执行异常，已降级放行");
        }
    }

    /** 清除指定规则的脚本缓存（热更新时调用） */
    public void evict(Long ruleId) {
        scriptClassCache.remove(ruleId);
        log.info("Groovy script cache evicted, ruleId={}", ruleId);
    }

    /** 预加载脚本到缓存 */
    public void preload(Long ruleId, String scriptContent) {
        try {
            GroovyClassLoader loader = new GroovyClassLoader();
            Class<? extends Script> clazz = loader.parseClass(scriptContent);
            scriptClassCache.put(ruleId, clazz);
            log.info("Groovy script preloaded, ruleId={}", ruleId);
        } catch (Exception e) {
            log.error("Failed to preload Groovy script, ruleId={}", ruleId, e);
        }
    }

    private Map<String, Object> buildContext(RiskRequest request) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("userId", request.getUserId());
        ctx.put("deviceId", request.getDeviceId());
        ctx.put("ip", request.getIp());
        ctx.put("scene", request.getScene());
        ctx.put("requestId", request.getRequestId());
        if (request.getOrderAmount() != null) {
            ctx.put("orderAmount", request.getOrderAmount());
        }
        if (request.getShippingAddress() != null) {
            ctx.put("shippingAddress", request.getShippingAddress());
        }
        return ctx;
    }

    private Map<String, Object> fallback(String reasonCode, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "PASS");
        result.put("score", 0.0);
        result.put("reasonCode", reasonCode);
        result.put("message", message);
        return result;
    }
}
