-- =============================================================
-- V2: 会话与消息表
-- Spec: docs/superpowers/specs/2026-07-07-ai-customer-service-design.md §4.2
-- 适用：MySQL 8.x，UTF8MB4 字符集
-- =============================================================

-- ---------- chat_session ----------
CREATE TABLE IF NOT EXISTS chat_session (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id        BIGINT       NOT NULL                COMMENT '租户ID（BaseEntity）',
    user_id          BIGINT       NOT NULL                COMMENT '会话所有者',
    channel          VARCHAR(32)  NOT NULL DEFAULT 'DEFAULT' COMMENT '渠道（v2启用：web/miniapp/wechat）',
    title            VARCHAR(255)                         COMMENT '会话标题（可空）',
    summary          TEXT                                COMMENT '滚动摘要（M4写入）',
    summary_version  INT          NOT NULL DEFAULT 0     COMMENT '摘要版本号（自增）',
    status           VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / ARCHIVED',
    create_time      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted          TINYINT      NOT NULL DEFAULT 0     COMMENT '逻辑删除（0=未删，1=已删）',
    PRIMARY KEY (id),
    KEY idx_tenant_user_status (tenant_id, user_id, status),
    KEY idx_tenant_user_updated (tenant_id, user_id, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话';

-- ---------- chat_message ----------
CREATE TABLE IF NOT EXISTS chat_message (
    id                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id          BIGINT        NOT NULL                COMMENT '租户ID（BaseEntity）',
    session_id         BIGINT        NOT NULL                COMMENT '所属会话ID',
    role               VARCHAR(16)   NOT NULL                COMMENT 'USER / ASSISTANT / SYSTEM / TOOL',
    content            TEXT          NOT NULL                COMMENT '消息文本',
    token_count        INT                                  COMMENT '消息token消耗',
    model_name         VARCHAR(64)                           COMMENT '产生此消息的模型名',
    trace_id           VARCHAR(64)                           COMMENT '链路追踪ID',
    tool_call_json     TEXT                                  COMMENT '工具调用JSON',
    citation_doc_ids   VARCHAR(1024)                         COMMENT 'RAG引用文档ID列表（逗号分隔）',
    create_time        DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time        DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted            TINYINT       NOT NULL DEFAULT 0     COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_tenant_session_created (tenant_id, session_id, create_time),
    KEY idx_tenant_session_trace (tenant_id, session_id, trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息';
