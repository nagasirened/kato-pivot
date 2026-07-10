-- =============================================================
-- V8: 操作审计表（spec §6 M11）
-- 用途：所有 admin 写操作（CREATE/UPDATE/DELETE/TRIGGER/APPROVE/REJECT）留痕
-- 与 V5 tool_call_audit 的区别：
--   - tool_call_audit: 工具调用（业务事件）
--   - op_audit:        管理员操作（管理事件）
-- =============================================================

CREATE TABLE IF NOT EXISTS op_audit (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id    BIGINT       NOT NULL                COMMENT '租户ID',
    user_id      BIGINT       NOT NULL                COMMENT '操作人ID',
    username     VARCHAR(64)  NOT NULL                COMMENT '操作人用户名（冗余便于查询）',
    action       VARCHAR(64)  NOT NULL                COMMENT 'CREATE/UPDATE/DELETE/TRIGGER/APPROVE/REJECT',
    resource     VARCHAR(64)  NOT NULL                COMMENT 'SYNC/KNOWLEDGE/TOOL/PROMPT/USER',
    resource_id  VARCHAR(64)  DEFAULT NULL            COMMENT '业务ID（可空）',
    request_uri  VARCHAR(255) NOT NULL                COMMENT 'HTTP 请求路径',
    http_method  VARCHAR(8)   NOT NULL                COMMENT 'GET/POST/PUT/DELETE',
    http_status  INT          DEFAULT NULL            COMMENT 'HTTP 状态码',
    args_json    TEXT         DEFAULT NULL            COMMENT '请求参数 JSON',
    result_json  TEXT         DEFAULT NULL            COMMENT '响应结果 JSON',
    trace_id     VARCHAR(64)  DEFAULT NULL            COMMENT '全链路 traceId',
    duration_ms  INT          DEFAULT NULL            COMMENT '执行耗时',
    create_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_tenant_user_time (tenant_id, user_id, create_time),
    KEY idx_tenant_resource (tenant_id, resource, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计表';
