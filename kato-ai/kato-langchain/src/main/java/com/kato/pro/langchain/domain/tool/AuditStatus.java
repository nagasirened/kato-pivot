package com.kato.pro.langchain.domain.tool;

/**
 * 工具调用审核状态机。
 *
 *   PENDING   → APPROVED  → EXECUTED
 *   PENDING   → REJECTED
 *   PENDING   → EXECUTED  (audit-auto-approve=true)
 *   EXECUTED  → FAILED    (执行异常)
 *
 * 终态：REJECTED / EXECUTED / FAILED
 * 中间态：PENDING / APPROVED（APPROVED 几乎瞬时进入 EXECUTED，作为审计窗口）
 */
public enum AuditStatus {
    PENDING, APPROVED, REJECTED, EXECUTED, FAILED;

    public boolean isTerminal() {
        return this == REJECTED || this == EXECUTED || this == FAILED;
    }
}
