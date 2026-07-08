package com.kato.pro.risk.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Groovy 脚本测试结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroovyTestResult {

    private String testCaseName;

    private boolean passed;

    private String output;

    private Long executionTimeMs;

    private String errorMessage;
}