-- =============================================================
-- V5: 工具调用框架（租户配置 + 调用审计）
-- Spec: docs/superpowers/specs/2026-07-07-ai-customer-service-design.md §7.3
-- =============================================================

-- 租户级工具启用配置（R2）
CREATE TABLE IF NOT EXISTS tenant_tool_config (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT      NOT NULL                COMMENT '租户ID',
    tool_name    VARCHAR(64) NOT NULL                COMMENT '工具名',
    enabled      TINYINT     NOT NULL DEFAULT 1     COMMENT '1=启用 / 0=停用',
    create_time  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted      TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_tool (tenant_id, tool_name, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户工具启用配置';

-- 工具调用审计表（A3 + 写类审核）
CREATE TABLE IF NOT EXISTS tool_call_audit (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT       NOT NULL                COMMENT '租户ID',
    tool_name    VARCHAR(64)  NOT NULL                COMMENT '工具名',
    tool_type    VARCHAR(16)  NOT NULL                COMMENT 'READ/WRITE',
    args_json    MEDIUMTEXT   NOT NULL                COMMENT '调用参数 JSON',
    requester_id BIGINT       NOT NULL                COMMENT '发起人 userId',
    channel      VARCHAR(32)  NOT NULL DEFAULT 'user' COMMENT '来源渠道',
    status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/EXECUTED/FAILED',
    approver_id  BIGINT       DEFAULT NULL              COMMENT '审核人 userId',
    approve_time DATETIME(3)  DEFAULT NULL              COMMENT '审核时间',
    result_json  MEDIUMTEXT   DEFAULT NULL              COMMENT '执行结果 JSON',
    error_msg    VARCHAR(1024) DEFAULT NULL             COMMENT '错误信息',
    trace_id     VARCHAR(64)  DEFAULT NULL              COMMENT '链路 traceId',
    create_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_tenant_status (tenant_id, status, create_time),
    KEY idx_tenant_tool (tenant_id, tool_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具调用审计';
