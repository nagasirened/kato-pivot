package com.kato.pro.risk.server.service;

import com.kato.pro.risk.client.dto.RiskRequest;
import com.kato.pro.risk.client.dto.RiskResponse;

/**
 * 规则执行引擎接口（Phase 1 骨架）。
 *
 * 执行流程：
 * 1. scene 白名单校验（REGISTER/LOGIN/MARKETING/ORDER/PAYMENT/REFUND），非法返回 400
 * 2. 规则匹配（按 scene + priority 排序）
 * 3. 配置规则 SpEL 执行
 * 4. Groovy 脚本执行（如有）
 * 5. 风险分数加权计算 → RiskResponse
 *
 * 热更新：reloadRule(ruleId, version) 清除本地 Groovy Class 缓存 + Caffeine 本地缓存。
 */
public interface RuleEngineService {

    /**
     * 执行风控检查。
     *
     * @param request 风控请求上下文
     * @return 风控决策响应（action/score/reasonCodes）
     */
    RiskResponse check(RiskRequest request);

    /**
     * 热更新触发：重新加载指定规则。
     * Kafka Consumer 收到 rule-updated 事件后调用。
     *
     * @param ruleId 规则 ID
     * @param version 新版本号
     */
    void reloadRule(Long ruleId, Integer version);

    /**
     * 测试 Groovy 脚本（不依赖规则ID，用于在线调试）。
     */
    java.util.Map<String, Object> testGroovyScript(Long ruleId, String scriptContent, RiskRequest request);
}
