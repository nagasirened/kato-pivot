package com.kato.pro.risk.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kato.pro.risk.server.entity.RiskCase;
import com.kato.pro.risk.server.mapper.RiskCaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 风控事件 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/risk/cases")
public class CaseController {

    @Autowired
    private RiskCaseMapper caseMapper;

    /**
     * 分页查询事件列表
     */
    @GetMapping
    public IPage<RiskCase> listCases(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LambdaQueryWrapper<RiskCase> query = new LambdaQueryWrapper<>();
        if (scene != null && !scene.isEmpty()) {
            query.eq(RiskCase::getScene, scene);
        }
        if (action != null && !action.isEmpty()) {
            query.eq(RiskCase::getAction, action);
        }
        if (userId != null && !userId.isEmpty()) {
            query.eq(RiskCase::getUserId, userId);
        }
        if (startDate != null && !startDate.isEmpty()) {
            query.ge(RiskCase::getCreatedAt, startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            query.le(RiskCase::getCreatedAt, endDate);
        }
        query.orderByDesc(RiskCase::getCreatedAt);

        return caseMapper.selectPage(new Page<>(page, size), query);
    }

    /**
     * 获取事件详情
     */
    @GetMapping("/{id}")
    public RiskCase getCase(@PathVariable Long id) {
        return caseMapper.selectById(id);
    }

    /**
     * 处理事件（更新状态）
     */
    @PutMapping("/{id}/handle")
    public RiskCase handleCase(@PathVariable Long id,
                               @RequestParam String handler,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String handleNote) {
        RiskCase riskCase = caseMapper.selectById(id);
        if (riskCase == null) {
            throw new RuntimeException("事件不存在: " + id);
        }

        if (status != null) {
            riskCase.setStatus(status);
        }
        if (handler != null) {
            riskCase.setHandler(handler);
        }
        if (handleNote != null) {
            riskCase.setHandleNote(handleNote);
        }
        riskCase.setHandleTime(LocalDateTime.now());
        caseMapper.updateById(riskCase);
        return riskCase;
    }
}