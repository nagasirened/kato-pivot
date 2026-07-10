package com.kato.pro.langchain.domain.prompt;

import java.util.List;

/**
 * DB 模板源 SPI。
 *
 *   - v1: 真实 PromptTemplateMapper 实现
 *   - 测试: 手写 stub
 */
public interface DbPromptSource {

    /** 加载全部非空模板（系统级 + 各租户） */
    List<PromptTemplateVo> loadAll();
}
