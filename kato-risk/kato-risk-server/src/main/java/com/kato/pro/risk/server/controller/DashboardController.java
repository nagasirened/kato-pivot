package com.kato.pro.risk.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kato.pro.risk.server.dto.DashboardStats;
import com.kato.pro.risk.server.entity.RiskCase;
import com.kato.pro.risk.server.entity.RiskRule;
import com.kato.pro.risk.server.mapper.RiskCaseMapper;
import com.kato.pro.risk.server.mapper.RiskRuleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仪表盘 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/risk/stats")
public class DashboardController {

    @Autowired
    private RiskCaseMapper caseMapper;

    @Autowired
    private RiskRuleMapper ruleMapper;

    @GetMapping("/dashboard")
    public DashboardStats getDashboard(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        // 默认近7天
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(7);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();

        // 今日统计
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().plusDays(1).atStartOfDay();

        LambdaQueryWrapper<RiskCase> todayQuery = new LambdaQueryWrapper<>();
        todayQuery.ge(RiskCase::getCreatedAt, todayStart);
        todayQuery.lt(RiskCase::getCreatedAt, todayEnd);
        List<RiskCase> todayCases = caseMapper.selectList(todayQuery);

        long todayTotal = todayCases.size();
        long todayPassed = todayCases.stream().filter(c -> "PASS".equals(c.getAction())).count();
        long todayBlocked = todayCases.stream().filter(c -> "BLOCK".equals(c.getAction())).count();
        long todayPending = todayCases.stream().filter(c -> "REVIEW".equals(c.getAction())).count();

        BigDecimal passRate = todayTotal > 0
                ? BigDecimal.valueOf(todayPassed * 100.0 / todayTotal)
                : BigDecimal.ZERO;
        BigDecimal blockRate = todayTotal > 0
                ? BigDecimal.valueOf(todayBlocked * 100.0 / todayTotal)
                : BigDecimal.ZERO;

        // 场景分布
        LambdaQueryWrapper<RiskCase> sceneQuery = new LambdaQueryWrapper<>();
        sceneQuery.ge(RiskCase::getCreatedAt, startTime);
        sceneQuery.lt(RiskCase::getCreatedAt, endTime);
        List<RiskCase> periodCases = caseMapper.selectList(sceneQuery);

        Map<String, Long> sceneCountMap = periodCases.stream()
                .collect(Collectors.groupingBy(RiskCase::getScene, Collectors.counting()));

        List<DashboardStats.SceneStat> sceneDistribution = sceneCountMap.entrySet().stream()
                .map(e -> DashboardStats.SceneStat.builder()
                        .scene(e.getKey())
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());

        // 趋势数据（每天）
        List<DashboardStats.TrendPoint> trendData = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            final LocalDate d = date;
            long passed = periodCases.stream()
                    .filter(c -> "PASS".equals(c.getAction())
                            && c.getCreatedAt() != null
                            && !c.getCreatedAt().isBefore(dayStart)
                            && c.getCreatedAt().isBefore(dayEnd))
                    .count();
            long blocked = periodCases.stream()
                    .filter(c -> "BLOCK".equals(c.getAction())
                            && c.getCreatedAt() != null
                            && !c.getCreatedAt().isBefore(dayStart)
                            && c.getCreatedAt().isBefore(dayEnd))
                    .count();

            trendData.add(DashboardStats.TrendPoint.builder()
                    .date(date.format(fmt))
                    .passed(passed)
                    .blocked(blocked)
                    .build());
        }

        // TOP 规则（按触发次数）
        List<RiskCase> allCases = caseMapper.selectList(null);
        Map<String, Long> reasonCodeCount = new HashMap<>();
        for (RiskCase c : allCases) {
            if (c.getReasonCodes() != null && !c.getReasonCodes().isEmpty()) {
                String[] codes = c.getReasonCodes().split(",");
                for (String code : codes) {
                    reasonCodeCount.merge(code.trim(), 1L, Long::sum);
                }
            }
        }

        List<DashboardStats.TopRule> topRules = reasonCodeCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Long ruleId = parseRuleId(e.getKey());
                    String ruleName = getRuleName(ruleId);
                    return DashboardStats.TopRule.builder()
                            .ruleId(ruleId)
                            .ruleName(ruleName != null ? ruleName : e.getKey())
                            .triggerCount(e.getValue())
                            .build();
                })
                .collect(Collectors.toList());

        return DashboardStats.builder()
                .todayTotal(todayTotal)
                .todayPassed(todayPassed)
                .todayBlocked(todayBlocked)
                .todayPending(todayPending)
                .passRate(passRate)
                .blockRate(blockRate)
                .sceneDistribution(sceneDistribution)
                .trendData(trendData)
                .topRules(topRules)
                .build();
    }

    private Long parseRuleId(String reasonCode) {
        try {
            return Long.parseLong(reasonCode.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0L;
        }
    }

    private String getRuleName(Long ruleId) {
        if (ruleId == null || ruleId <= 0) {
            return null;
        }
        RiskRule rule = ruleMapper.selectById(ruleId);
        return rule != null ? rule.getName() : null;
    }
}