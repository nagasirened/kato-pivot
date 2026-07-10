package com.kato.pro.langchain.domain.prompt;

/**
 * 内存中的不可变模板（registry / renderer 专用）。
 *
 *   - tenantId = null  → 系统级（YAML / DB 系统级）
 *   - tenantId != null  → 租户级覆盖
 *   - source             → "BUILTIN" / "YAML" / "DB"
 */
public record PromptTemplateVo(Long tenantId, String key, String content,
                                Integer version, String source) {}
