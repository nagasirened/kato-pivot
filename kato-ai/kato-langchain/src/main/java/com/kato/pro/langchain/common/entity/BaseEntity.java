package com.kato.pro.langchain.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 所有业务实体（除 Tenant 自身）的基类。
 *
 * 字段说明：
 *   - id: 自增主键
 *   - tenantId: 多租户隔离字段，由 TenantMybatisInterceptor 自动注入（INSERT）
 *                            并自动追加 WHERE 条件（SELECT/UPDATE/DELETE）
 *   - createTime / updateTime: 由 MetaObjectHandler 自动填充
 *   - deleted: 逻辑删除标记（0=未删，1=已删），MyBatis-Plus 自动追加 WHERE deleted=0
 *
 * Tenant 表不能继承本类（它是租户自身，无自身 tenant_id）。
 */
@Data
public abstract class BaseEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 多租户隔离字段。INSERT 时由 TenantMybatisInterceptor 从 TenantContext 自动填入，业务代码无需 set。 */
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
