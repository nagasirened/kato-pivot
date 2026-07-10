package com.kato.pro.langchain.domain.prompt;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kato.pro.langchain.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Prompt 模板（DB 表）。
 *
 *   - tenant_id = NULL  表示系统级模板（所有租户可见，YAML 之外的兜底）
 *   - tenant_id != NULL 表示租户级覆盖（DB > YAML 优先级）
 *   - (tenant_id, key, deleted) 唯一约束：同租户同一 key 只能有一条未删记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prompt_template")
public class PromptTemplate extends BaseEntity {

    /** 模板 key（如 "rag_chat" / "chitchat" / "tool_decision"） */
    private String templateKey;

    /** 模板内容（包含 ${var} 占位符） */
    private String content;

    /** 版本号（自增，每次更新 +1） */
    private Integer version;

    /** 描述（admin 看） */
    private String description;
}
