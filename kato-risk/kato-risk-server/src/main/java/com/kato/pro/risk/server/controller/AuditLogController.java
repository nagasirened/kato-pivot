package com.kato.pro.risk.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kato.pro.risk.server.entity.RiskRuleAudit;
import com.kato.pro.risk.server.mapper.RiskRuleAuditMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审核日志 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/risk/audit-logs")
public class AuditLogController {

    @Autowired
    private RiskRuleAuditMapper auditMapper;

    /**
     * 分页查询审核日志
     */
    @GetMapping
    public IPage<RiskRuleAudit> listAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long ruleId) {

        LambdaQueryWrapper<RiskRuleAudit> query = new LambdaQueryWrapper<>();
        if (ruleId != null) {
            query.eq(RiskRuleAudit::getRuleId, ruleId);
        }
        query.orderByDesc(RiskRuleAudit::getOperateAt);

        return auditMapper.selectPage(new Page<>(page, size), query);
    }
}