package com.kato.pro.risk.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 规则变更审计日志。
 */
@Data
@TableName("risk_rule_audit")
public class RiskRuleAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;

    /** CREATE / UPDATE / DELETE / ENABLE / DISABLE */
    private String operation;

    private String operator;

    /** 变更前内容 */
    private String oldContent;

    /** 变更后内容 */
    private String newContent;

    private LocalDateTime operateAt;
}