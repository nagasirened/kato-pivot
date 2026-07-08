package com.kato.pro.risk.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kato.pro.risk.server.dto.GroovyTestRequest;
import com.kato.pro.risk.server.dto.GroovyTestResult;
import com.kato.pro.risk.server.dto.RuleSaveRequest;
import com.kato.pro.risk.server.entity.RiskRule;
import com.kato.pro.risk.server.entity.RiskRuleAudit;
import com.kato.pro.risk.server.mapper.RiskRuleAuditMapper;
import com.kato.pro.risk.server.mapper.RiskRuleMapper;
import com.kato.pro.risk.server.service.RuleEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 风控规则管理 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/risk/rules")
public class RuleController {

    @Autowired
    private RiskRuleMapper ruleMapper;

    @Autowired
    private RiskRuleAuditMapper auditMapper;

    @Autowired
    private RuleEngineService ruleEngine;

    /**
     * 分页查询规则列表
     */
    @GetMapping
    public IPage<RiskRule> listRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) Boolean enabled) {

        LambdaQueryWrapper<RiskRule> query = new LambdaQueryWrapper<>();
        if (scene != null && !scene.isEmpty()) {
            query.eq(RiskRule::getScene, scene);
        }
        if (ruleType != null && !ruleType.isEmpty()) {
            query.eq(RiskRule::getRuleType, ruleType);
        }
        if (enabled != null) {
            query.eq(RiskRule::getEnabled, enabled);
        }
        query.orderByAsc(RiskRule::getPriority);

        return ruleMapper.selectPage(new Page<>(page, size), query);
    }

    /**
     * 获取单条规则详情
     */
    @GetMapping("/{id}")
    public RiskRule getRule(@PathVariable Long id) {
        return ruleMapper.selectById(id);
    }

    /**
     * 新建规则
     */
    @PostMapping
    public RiskRule createRule(@Valid @RequestBody RuleSaveRequest req) {
        RiskRule rule = new RiskRule();
        rule.setName(req.getName());
        rule.setScene(req.getScene());
        rule.setRuleType(req.getRuleType());
        rule.setContent(req.getContent());
        rule.setPriority(req.getPriority() != null ? req.getPriority() : 99);
        rule.setEnabled(req.getEnabled() != null ? req.getEnabled() : true);
        rule.setAbGroup(req.getAbGroup());
        rule.setCreatedBy(req.getCreatedBy() != null ? req.getCreatedBy() : "system");
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        rule.setVersion(1);

        ruleMapper.insert(rule);
        saveAudit(rule.getId(), "CREATE", rule.getCreatedBy(), null, rule.getContent());
        return rule;
    }

    /**
     * 更新规则
     */
    @PutMapping("/{id}")
    public RiskRule updateRule(@PathVariable Long id, @Valid @RequestBody RuleSaveRequest req) {
        RiskRule existing = ruleMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("规则不存在: " + id);
        }

        String oldContent = existing.getContent();
        existing.setName(req.getName());
        existing.setScene(req.getScene());
        existing.setRuleType(req.getRuleType());
        existing.setContent(req.getContent());
        if (req.getPriority() != null) {
            existing.setPriority(req.getPriority());
        }
        if (req.getEnabled() != null) {
            existing.setEnabled(req.getEnabled());
        }
        existing.setAbGroup(req.getAbGroup());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setVersion(existing.getVersion() + 1);

        ruleMapper.updateById(existing);
        saveAudit(id, "UPDATE", req.getCreatedBy() != null ? req.getCreatedBy() : "system", oldContent, existing.getContent());

        // 热更新规则
        ruleEngine.reloadRule(id, existing.getVersion());
        return existing;
    }

    /**
     * 删除规则
     */
    @DeleteMapping("/{id}")
    public void deleteRule(@PathVariable Long id) {
        RiskRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            return;
        }
        ruleMapper.deleteById(id);
        saveAudit(id, "DELETE", "system", rule.getContent(), null);
    }

    /**
     * 启用/禁用规则
     */
    @PutMapping("/{id}/toggle")
    public RiskRule toggleRule(@PathVariable Long id, @RequestParam(required = false) Boolean enabled) {
        RiskRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new RuntimeException("规则不存在: " + id);
        }

        boolean newEnabled = enabled != null ? enabled : !rule.getEnabled();
        rule.setEnabled(newEnabled);
        rule.setUpdatedAt(LocalDateTime.now());
        ruleMapper.updateById(rule);

        saveAudit(id, newEnabled ? "ENABLE" : "DISABLE", "system", null, null);
        ruleEngine.reloadRule(id, rule.getVersion());
        return rule;
    }

    /**
     * 测试 Groovy 脚本
     */
    @PostMapping("/test")
    public GroovyTestResult testScript(@RequestBody GroovyTestRequest req) {
        long startMs = System.currentTimeMillis();
        try {
            // 构建测试请求
            com.kato.pro.risk.client.dto.RiskRequest riskRequest = new com.kato.pro.risk.client.dto.RiskRequest();
            riskRequest.setScene(req.getScene());
            if (req.getInputContext() != null) {
                riskRequest.setUserId(req.getInputContext().get("userId"));
                riskRequest.setDeviceId(req.getInputContext().get("deviceId"));
                riskRequest.setIp(req.getInputContext().get("ip"));
                riskRequest.setRequestId(req.getInputContext().get("requestId"));
                if (req.getInputContext().containsKey("orderAmount")) {
                    try {
                        riskRequest.setOrderAmount(new java.math.BigDecimal(req.getInputContext().get("orderAmount")));
                    } catch (NumberFormatException ignored) {}
                }
                riskRequest.setShippingAddress(req.getInputContext().get("shippingAddress"));
            }

            // 执行 Groovy 脚本
            java.util.Map<String, Object> groovyResult = ruleEngine.testGroovyScript(
                    -1L, req.getScriptContent(), riskRequest);

            String action = String.valueOf(groovyResult.getOrDefault("action", "PASS"));
            boolean passed = !"PASS".equals(action) ? false : true;
            long execMs = System.currentTimeMillis() - startMs;

            return GroovyTestResult.builder()
                    .testCaseName(req.getTestCaseName() != null ? req.getTestCaseName() : "test")
                    .passed(passed)
                    .output(action + " | score=" + groovyResult.getOrDefault("score", 0.0))
                    .executionTimeMs(execMs)
                    .build();
        } catch (Exception e) {
            log.error("[RuleController] testScript failed", e);
            long execMs = System.currentTimeMillis() - startMs;
            return GroovyTestResult.builder()
                    .testCaseName(req.getTestCaseName() != null ? req.getTestCaseName() : "test")
                    .passed(false)
                    .errorMessage(e.getMessage())
                    .executionTimeMs(execMs)
                    .build();
        }
    }

    private void saveAudit(Long ruleId, String operation, String operator, String oldContent, String newContent) {
        RiskRuleAudit audit = new RiskRuleAudit();
        audit.setRuleId(ruleId);
        audit.setOperation(operation);
        audit.setOperator(operator);
        audit.setOldContent(oldContent);
        audit.setNewContent(newContent);
        audit.setOperateAt(LocalDateTime.now());
        auditMapper.insert(audit);
    }
}