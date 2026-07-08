package com.kato.pro.langchain.domain.chat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 模型路由配置：定义每种任务类型使用哪个模型名。
 * 实际请求时，ModelRouter 用 model name 调用 MiniMaxClient。
 */
@Data
@ConfigurationProperties(prefix = "minimax.models")
public class ModelProperties {

    /** 简单任务（意图分类/摘要/改写）使用的模型 */
    private String simple = "m2.5";

    /** 复杂任务（主对话/工具调用决策）使用的模型 */
    private String complex = "m3";
}
