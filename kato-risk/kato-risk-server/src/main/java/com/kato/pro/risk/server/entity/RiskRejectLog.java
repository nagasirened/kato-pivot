package com.kato.pro.risk.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 风控拒绝日志（每次 BLOCK 决策必须写入）。
 */
@Data
@TableName("risk_reject_log")
public class RiskRejectLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String requestId;

    private String userId;

    private String scene;

    private String action;

    private BigDecimal riskScore;

    /** 命中的规则列表 JSON */
    private String hitRules;

    private String reasonCodes;

    /** 请求上下文 JSON */
    private String requestContext;

    /** 规则类型：CONFIG / GROOVY */
    private String ruleType;

    /** Groovy 脚本版本 */
    private String scriptVersion;

    /** A/B 实验分组 */
    private String abGroup;

    private LocalDateTime createdAt;
}