package com.kato.pro.risk.server.dto;

import lombok.Data;
import java.util.Map;

/**
 * Groovy 脚本测试请求
 */
@Data
public class GroovyTestRequest {

    private String scene;

    private Map<String, String> inputContext;

    private String scriptContent;

    private String testCaseName;
}