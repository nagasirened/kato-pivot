package com.kato.pro.risk.client.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * Groovy 脚本单元测试结果。
 *
 * Phase 1 骨架。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroovyTestResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 测试用例名称 */
    private String testCaseName;

    /** 是否通过 */
    private boolean passed;

    /** 实际返回 action */
    private String actualAction;

    /** 实际风险分 */
    private Double actualScore;

    /** 执行耗时 ms */
    private Long executionMs;

    /** 错误信息（失败时） */
    private String errorMessage;

    /** 整体通过率（所有用例） */
    private Double passRate;
}