-- =============================================================
-- V7: 用户与角色表 + 种子数据
-- Spec: docs/superpowers/specs/2026-07-07-ai-customer-service-design.md §4.1 + M11
-- 说明：V1 在 kato-langchain 项目中之前未落地（其他模块的 V1 互不干扰）
-- =============================================================

CREATE TABLE IF NOT EXISTS user (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT       NOT NULL                COMMENT '租户ID',
    username      VARCHAR(64)  NOT NULL                COMMENT '用户名',
    password_hash VARCHAR(128) NOT NULL                COMMENT 'SHA-256 hex of "<username>-pass"',
    role          VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT 'ADMIN/OPERATOR/USER',
    status        VARCHAR(16)  NOT NULL DEFAULT 'ENABLE' COMMENT 'ENABLE/DISABLE',
    channel       VARCHAR(32)  DEFAULT NULL            COMMENT '渠道（v2 启用）',
    create_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_username (tenant_id, username, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 3 个默认账号（密码 = "<username>-pass" 的 SHA-256）
INSERT IGNORE INTO user (id, tenant_id, username, password_hash, role) VALUES
(1, 1, 'admin', 'b630f5d579dfef28c45ddf5e3c7a65f09ebca4d5b064a70c4203578c8667fdeb', 'ADMIN'),
(2, 1, 'op',    'f39f931be5c197c37376d7fbc3f7afdc62296f5d49adf1ea8a9ac51c3d947451', 'OPERATOR'),
(3, 1, 'user1', 'ef9ce22f3c2cdda7968af2a110078df197b101f3602c7f12cd6c41bc501f6fdd', 'USER');
