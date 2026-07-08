package com.kato.pro.risk.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {

    private Long todayTotal;

    private Long todayPassed;

    private Long todayBlocked;

    private Long todayPending;

    private BigDecimal passRate;

    private BigDecimal blockRate;

    private List<SceneStat> sceneDistribution;

    private List<TrendPoint> trendData;

    private List<TopRule> topRules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SceneStat {
        private String scene;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String date;
        private Long passed;
        private Long blocked;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopRule {
        private Long ruleId;
        private String ruleName;
        private Long triggerCount;
    }
}