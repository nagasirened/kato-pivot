package com.kato.pro.sensitive.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kato.pro.base.entity.Result;
import com.kato.pro.sensitive.entity.SensitiveWordAudit;
import com.kato.pro.sensitive.service.ISensitiveWordAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 敏感词审核控制器
 */
@Slf4j
@RestController
@RequestMapping("/sensitive/v1/audit")
public class SensitiveWordAuditController {

    @Resource
    private ISensitiveWordAuditService auditService;

    /**
     * 提交审核申请
     */
    @PostMapping("/apply")
    public Result<Boolean> apply(@RequestBody SensitiveWordAudit audit) {
        if (audit == null || audit.getWord() == null || audit.getWord().isEmpty()) {
            return Result.build(1, "敏感词不能为空");
        }
        boolean success = auditService.apply(audit);
        return Result.build(success);
    }

    /**
     * 待审核列表
     */
    @GetMapping("/page")
    public Result<IPage<SensitiveWordAudit>> pagePending(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        IPage<SensitiveWordAudit> page = auditService.pagePending(pageNum, pageSize);
        return Result.build(page);
    }

    /**
     * 审批通过
     */
    @PostMapping("/approve")
    public Result<Boolean> approve(
            @RequestParam Long auditId,
            @RequestParam(required = false) Integer auditorId,
            @RequestParam(required = false) String auditRemark) {
        boolean success = auditService.approve(auditId, auditorId, auditRemark);
        return Result.build(success);
    }

    /**
     * 审批拒绝
     */
    @PostMapping("/reject")
    public Result<Boolean> reject(
            @RequestParam Long auditId,
            @RequestParam(required = false) Integer auditorId,
            @RequestParam(required = false) String auditRemark) {
        boolean success = auditService.reject(auditId, auditorId, auditRemark);
        return Result.build(success);
    }
}
