-- =============================================================
-- V6: 知识库同步运行记录
-- Spec: docs/superpowers/specs/2026-07-07-ai-customer-service-design.md §6 (M9)
-- =============================================================

CREATE TABLE IF NOT EXISTS sync_run_record (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      BIGINT       NOT NULL                COMMENT '租户ID',
    adapter_name   VARCHAR(64)  NOT NULL                COMMENT '适配器名',
    start_time     DATETIME(3)  NOT NULL                COMMENT '开始时间',
    end_time       DATETIME(3)  DEFAULT NULL              COMMENT '结束时间',
    success_count  INT          NOT NULL DEFAULT 0     COMMENT '成功数',
    fail_count     INT          NOT NULL DEFAULT 0     COMMENT '失败数',
    status         VARCHAR(16)  NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/SUCCESS/PARTIAL/FAILED',
    error_msg      VARCHAR(2048) DEFAULT NULL             COMMENT '错误信息',
    create_time    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_tenant_adapter (tenant_id, adapter_name, start_time),
    KEY idx_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='同步运行记录';
