package com.kato.pro.langchain.domain.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计注解（M11）。
 *
 * 标注在 controller 方法上：由 OpAuditAspect 拦截并写 op_audit 表。
 * 通常与 @RequireRole 配合使用。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpAuditLog {
    String action();    // CREATE/UPDATE/DELETE/TRIGGER/APPROVE/REJECT
    String resource();  // SYNC/KNOWLEDGE/TOOL/PROMPT/USER
}
