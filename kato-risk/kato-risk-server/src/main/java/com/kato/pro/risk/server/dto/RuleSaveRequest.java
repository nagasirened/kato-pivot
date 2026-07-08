package com.kato.pro.risk.server.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 规则创建/更新请求
 */
@Data
public class RuleSaveRequest {

    @NotBlank(message = "规则名称不能为空")
    private String name;

    @NotBlank(message = "场景不能为空")
    private String scene;

    @NotBlank(message = "规则类型不能为空")
    private String ruleType; // CONFIG / GROOVY

    @NotBlank(message = "规则内容不能为空")
    private String content;

    private Integer priority;

    private Boolean enabled = true;

    private String abGroup;

    private String createdBy;
}