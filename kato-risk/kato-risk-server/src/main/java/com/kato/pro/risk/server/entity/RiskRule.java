package com.kato.pro.risk.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 风控规则实体。
 * content 字段存 SpEL 表达式（配置规则）或 Groovy 脚本（动态规则）。
 */
@Data
@TableName("risk_rule")
public class RiskRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则名称 */
    private String name;

    /** 环节：REGISTER / LOGIN / MARKETING / ORDER / PAYMENT / REFUND */
    private String scene;

    /** 规则类型：CONFIG / GROOVY */
    private String ruleType;

    /** 规则内容（SpEL 表达式 或 Groovy 脚本） */
    private String content;

    /** 优先级，数字越小越优先执行 */
    private Integer priority;

    /** 是否启用 */
    private Boolean enabled;

    /** 版本号，每次编辑 +1 */
    @Version
    private Integer version;

    /** A/B 实验分组，如 "10%" */
    private String abGroup;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}