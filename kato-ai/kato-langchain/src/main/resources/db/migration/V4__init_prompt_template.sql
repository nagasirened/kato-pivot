-- =============================================================
-- V4: Prompt 模板表
-- Spec: docs/superpowers/specs/2026-07-07-ai-customer-service-design.md §7.3
-- =============================================================

CREATE TABLE IF NOT EXISTS prompt_template (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT       DEFAULT NULL              COMMENT '租户ID（NULL=系统级）',
    template_key  VARCHAR(64)  NOT NULL                COMMENT '模板 key',
    content       MEDIUMTEXT   NOT NULL                COMMENT '模板内容（含 ${var}）',
    version       INT          NOT NULL DEFAULT 1     COMMENT '版本号',
    description   VARCHAR(255)                         COMMENT '描述',
    create_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_key (tenant_id, template_key, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prompt 模板';
