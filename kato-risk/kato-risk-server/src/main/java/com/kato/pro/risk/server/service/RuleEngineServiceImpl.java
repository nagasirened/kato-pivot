package com.kato.pro.risk.server.service;

import com.kato.pro.risk.client.dto.RiskRequest;
import com.kato.pro.risk.client.dto.RiskResponse;
import com.kato.pro.risk.server.algorithm.GroovyScriptExecutor;
import com.kato.pro.risk.server.entity.RiskRejectLog;
import com.kato.pro.risk.server.entity.RiskRule;
import com.kato.pro.risk.server.mapper.RiskRejectLogMapper;
import com.kato.pro.risk.server.mapper.RiskRuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;


/**
 * 规则执行引擎实现。
 */
@Slf4j
@Service
public class RuleEngineServiceImpl implements RuleEngineService {

    private static final Set<String> VALID_SCENES = new HashSet<>(
            Arrays.asList("REGISTER", "LOGIN", "MARKETING", "ORDER", "PAYMENT", "REFUND"));

    private final ExpressionParser spelParser = new SpelExpressionParser();

    /** Caffeine 本地规则结果缓存：requestId -> RiskResponse */
    private final Cache<String, RiskResponse> ruleResultCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(java.time.Duration.ofMinutes(5))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RiskRuleMapper riskRuleMapper;
    @Autowired
    private RiskRejectLogMapper riskRejectLogMapper;
    @Autowired
    private GroovyScriptExecutor groovyScriptExecutor;

    @Override
    public RiskResponse check(RiskRequest request) {
        String scene = request.getScene();
        if (!VALID_SCENES.contains(scene)) {
            throw new IllegalArgumentException("未知环节: " + scene);
        }

        // 1. Caffeine 缓存查询（幂等）
        if (request.getRequestId() != null) {
            RiskResponse cached = ruleResultCache.getIfPresent(request.getRequestId());
            if (cached != null) {
                return cached;
            }
        }

        // 2. 从 MySQL 加载该环节所有启用规则
        List<RiskRule> rules = riskRuleMapper.findActiveRulesByScene(scene);
        if (rules.isEmpty()) {
            RiskResponse pass = RiskResponse.pass(request.getRequestId());
            cacheIfNeeded(request, pass);
            return pass;
        }

        // 3. 按优先级顺序执行配置规则
        StandardEvaluationContext evalCtx = buildSpelContext(request);
        StringBuilder hitRules = new StringBuilder();
        BigDecimal totalScore = BigDecimal.ZERO;
        List<String> reasonCodes = new ArrayList<>();

        for (RiskRule rule : rules) {
            if ("CONFIG".equals(rule.getRuleType())) {
                executeConfigRule(rule, evalCtx, hitRules, totalScore, reasonCodes);
            } else if ("GROOVY".equals(rule.getRuleType())) {
                executeGroovyRule(rule, request, hitRules, totalScore, reasonCodes);
            }

            // 命中 BLOCK 短路
            if (totalScore.compareTo(new BigDecimal("1.0")) >= 0) {
                break;
            }
        }

        // 4. 构建响应
        String action = totalScore.compareTo(new BigDecimal("0.7")) >= 0 ? "BLOCK"
                        : totalScore.compareTo(new BigDecimal("0.3")) >= 0 ? "REVIEW"
                        : "PASS";

        RiskResponse response = new RiskResponse();
        response.setAction(action);
        response.setScore(totalScore.multiply(BigDecimal.valueOf(100)).intValue());
        response.setReasonCodes(reasonCodes);
        response.setRequestId(request.getRequestId());
        response.setMessage(action.equals("PASS") ? "风控检查通过" : "风险偏高，请联系客服");
        response.setExtData(new java.util.HashMap<>());

        // 5. BLOCK 决策必须落库
        if ("BLOCK".equals(action)) {
            saveRejectLog(request, response, hitRules.toString());
        }

        cacheIfNeeded(request, response);
        return response;
    }

    @Override
    @Transactional
    public void reloadRule(Long ruleId, Integer version) {
        // 清除 Caffeine 缓存
        // 注意：这里简化处理，实际应按 requestId 部分清除
        ruleResultCache.invalidateAll();

        // 清除 Groovy 脚本缓存
        groovyScriptExecutor.evict(ruleId);

        // 更新 MySQL 中规则版本（乐观锁）
        if (version != null) {
            RiskRule rule = riskRuleMapper.findById(ruleId);
            if (rule != null) {
                rule.setVersion(version);
                riskRuleMapper.updateById(rule);
            }
        }

        log.info("Rule hot-reload completed, ruleId={}, version={}", ruleId, version);
    }

    @Override
    public Map<String, Object> testGroovyScript(Long ruleId, String scriptContent, RiskRequest request) {
        return groovyScriptExecutor.execute(ruleId, scriptContent, request);
    }

    private void executeConfigRule(RiskRule rule, StandardEvaluationContext ctx,
                                   StringBuilder hitRules, BigDecimal totalScore, List<String> reasonCodes) {
        try {
            Expression exp = spelParser.parseExpression(rule.getContent());
            Boolean result = exp.getValue(ctx, Boolean.class);
            if (Boolean.TRUE.equals(result)) {
                if (hitRules.length() > 0) hitRules.append(",");
                hitRules.append(rule.getName());
                reasonCodes.add(rule.getName());
                totalScore = totalScore.add(new BigDecimal("0.3")); // 配置规则每次命中 +0.3
            }
        } catch (Exception e) {
            log.warn("Config rule evaluation error, ruleId={}", rule.getId(), e);
        }
    }

    private void executeGroovyRule(RiskRule rule, RiskRequest request,
                                    StringBuilder hitRules, BigDecimal totalScore, List<String> reasonCodes) {
        try {
            Map<String, Object> groovyResult = groovyScriptExecutor.execute(
                    rule.getId(), rule.getContent(), request);

            String ruleAction = String.valueOf(groovyResult.getOrDefault("action", "PASS"));
            if (!"PASS".equals(ruleAction)) {
                if (hitRules.length() > 0) hitRules.append(",");
                hitRules.append(rule.getName());
                reasonCodes.add(String.valueOf(groovyResult.getOrDefault("reasonCode", rule.getName())));
                Object score = groovyResult.getOrDefault("score", 0.0);
                totalScore = totalScore.add(new BigDecimal(score.toString()));
            }
        } catch (Exception e) {
            log.warn("Groovy rule execution error, ruleId={}", rule.getId(), e);
        }
    }

    @Transactional
    public void saveRejectLog(RiskRequest request, RiskResponse response, String hitRules) {
        try {
            RiskRejectLog log = new RiskRejectLog();
            log.setRequestId(request.getRequestId());
            log.setUserId(request.getUserId());
            log.setScene(request.getScene());
            log.setAction(response.getAction());
            log.setRiskScore(new BigDecimal(String.valueOf(response.getScore())));
            log.setReasonCodes(String.join(",", response.getReasonCodes()));
            log.setHitRules(hitRules);
            log.setRequestContext(objectMapper.writeValueAsString(request));
            log.setRuleType("MIXED");
            log.setCreatedAt(LocalDateTime.now());
            riskRejectLogMapper.insert(log);
        } catch (Exception e) {
            // 降级：写入内存 buffer，定时重试（此处简化处理）
            log.error("Failed to write risk_reject_log, requestId={}", request.getRequestId(), e);
        }
    }

    private StandardEvaluationContext buildSpelContext(RiskRequest request) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        ctx.setVariable("userId", request.getUserId());
        ctx.setVariable("deviceId", request.getDeviceId());
        ctx.setVariable("ip", request.getIp());
        ctx.setVariable("scene", request.getScene());
        ctx.setVariable("orderAmount", request.getOrderAmount());
        ctx.setVariable("shippingAddress", request.getShippingAddress());
        return ctx;
    }

    private void cacheIfNeeded(RiskRequest request, RiskResponse response) {
        if (request.getRequestId() != null) {
            ruleResultCache.put(request.getRequestId(), response);
        }
    }
}