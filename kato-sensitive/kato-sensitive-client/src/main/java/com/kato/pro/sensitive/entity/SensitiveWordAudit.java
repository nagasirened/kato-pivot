package com.kato.pro.sensitive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 敏感词审核实体
 */
@Data
@TableName("sensitive_word_audit")
public class SensitiveWordAudit {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 敏感词
     */
    private String word;

    /**
     * 敏感等级：URGENT-紧急, MEDIUM-中等, NORMAL-普通
     */
    private String level;

    /**
     * 分类
     */
    private String category;

    /**
     * 状态：ENABLE-启用, DISABLE-禁用
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 审核状态：PENDING-待审核, APPROVED-已通过, REJECTED-已拒绝
     */
    private String auditStatus;

    /**
     * 申请人ID
     */
    private Integer applicantId;

    /**
     * 审核人ID
     */
    private Integer auditorId;

    /**
     * 审核时间
     */
    private LocalDateTime auditTime;

    /**
     * 审核备注
     */
    private String auditRemark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
