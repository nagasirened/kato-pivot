package com.kato.pro.langchain.common.security;

/**
 * 角色枚举（spec §6 M11）。
 *
 *   - ADMIN    : 平台超级管理员（所有权限）
 *   - OPERATOR : 租户运营（业务查询 + 审核 + 部分配置）
 *   - USER     : 普通用户（仅业务查询）
 *
 * v1 字符串持久化在 user.role 字段；v2 引入 RBAC 表。
 */
public enum Role {
    ADMIN,
    OPERATOR,
    USER;

    public static Role fromString(String s) {
        if (s == null || s.isBlank()) return USER;
        try {
            return Role.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }
}
