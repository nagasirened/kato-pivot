package com.kato.pro.langchain.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户实体（独立，不继承 BaseEntity）。
 *
 * 设计理由：BaseEntity 含 tenant_id 字段，但 Tenant 表本身就是租户维度，
 * 让 Tenant.tenantId 指向自身会造成语义混乱。Tenant 表独立维护自身的审计字段。
 */
@Data
@TableName("tenant")
public class Tenant implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
