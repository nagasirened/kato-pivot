package com.kato.pro.risk.client.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Map;

/**
 * Groovy 脚本单元测试请求。
 *
 * Phase 1 骨架。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroovyTestRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 脚本 ID（为空时直接测试脚本内容） */
    private String scriptId;

    /** 脚本内容（测试用） */
    private String scriptContent;

    /** 测试用例输入（key-value） */
    private Map<String, Object> testInput;

    /** 期望的 action（PASS / REVIEW / BLOCK） */
    private String expectedAction;
}