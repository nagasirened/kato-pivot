package com.kato.pro.risk.server.context;

import com.kato.pro.risk.client.dto.RiskRequest;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 风控检查上下文构建工具类。
 *
 * 封装 RiskRequest + 中间计算状态（命中规则列表、累计分数、原因码）。
 * 在规则引擎执行过程中层层传递，避免大量参数透传。
 *
 * 用法：
 * RiskCheckContext ctx = RiskCheckContext.of(request);
 * ctx.addHitRule("IP黑名单").addScore(BigDecimal.valueOf(0.3));
 * if (ctx.getTotalScore().compareTo(BigDecimal.ONE) >= 0) { // 命中 BLOCK
 *     ctx.shortCircuit();
 * }
 *
 * Phase 1: 纯工具类，不涉及业务逻辑。
 * 业务扩展时可在 builder 上增加 cache 前缀、traceId 等字段。
 */
@Data
@Builder
public class RiskCheckContext {

    private RiskRequest request;
    @Builder.Default
    private List<String> hitRules = new ArrayList<>();
    @Builder.Default
    private List<String> reasonCodes = new ArrayList<>();
    @Builder.Default
    private BigDecimal totalScore = BigDecimal.ZERO;
    @Builder.Default
    private boolean shortCircuited = false;

    /**
     * 静态工厂方法：从 RiskRequest 构建上下文。
     */
    public static RiskCheckContext of(RiskRequest request) {
        return RiskCheckContext.builder()
                .request(request)
                .hitRules(new ArrayList<>())
                .reasonCodes(new ArrayList<>())
                .totalScore(BigDecimal.ZERO)
                .shortCircuited(false)
                .build();
    }

    /**
     * 添加命中规则。
     */
    public void addHitRule(String ruleName) {
        if (ruleName != null && !ruleName.isEmpty()) {
            this.hitRules.add(ruleName);
        }
    }

    /**
     * 添加原因码。
     */
    public void addReasonCode(String reasonCode) {
        if (reasonCode != null && !reasonCode.isEmpty()) {
            this.reasonCodes.add(reasonCode);
        }
    }

    /**
     * 累加风险分数。
     */
    public void addScore(BigDecimal score) {
        if (score != null) {
            this.totalScore = this.totalScore.add(score);
        }
    }

    /**
     * 短路：命中 BLOCK 等级，直接终止后续规则评估。
     */
    public void shortCircuit() {
        this.shortCircuited = true;
    }

    /**
     * 判断是否已短路。
     */
    public boolean isShortCircuited() {
        return this.shortCircuited;
    }

    /**
     * 判断是否已达到 BLOCK 阈值（>= 1.0）。
     */
    public boolean isBlockThreshold() {
        return this.totalScore.compareTo(BigDecimal.ONE) >= 0;
    }

    /**
     * 获取逗号分隔的命中规则字符串。
     */
    public String getHitRulesStr() {
        return String.join(",", this.hitRules);
    }

    /**
     * 获取逗号分隔的原因码字符串。
     */
    public String getReasonCodesStr() {
        return String.join(",", this.reasonCodes);
    }
}
