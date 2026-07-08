package com.kato.pro.risk.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 风控案件表。
 */
@Data
@TableName("risk_case")
public class RiskCase implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    /** 环节 */
    private String scene;

    /** 请求唯一 ID */
    private String requestId;

    /** 决策：PASS / REVIEW / BLOCK */
    private String action;

    /** 风险分 0.0~1.0 */
    private BigDecimal riskScore;

    /** 命中的规则编码列表 */
    private String reasonCodes;

    /** 请求上下文 JSON */
    private String requestContext;

    /** 状态：PENDING / HANDLED / REJECTED / REVOKED */
    private String status;

    /** 处理人 */
    private String handler;

    /** 处理备注 */
    private String handleNote;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 创建时间 */
    private LocalDateTime createdAt;
}