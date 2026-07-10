package com.kato.pro.langchain.domain.tool;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kato.pro.langchain.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户级工具启用配置（R2）。
 *
 *   - toolName : 工具名（对应 @Tool.name）
 *   - enabled  : true=启用 / false=停用；新建租户默认开启系统级全部工具
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tenant_tool_config")
public class TenantToolConfig extends BaseEntity {

    private String toolName;
    private Boolean enabled;
}
