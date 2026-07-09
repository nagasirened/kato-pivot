-- =============================================================
-- V3: 知识库文档与分段
-- Spec: docs/superpowers/specs/2026-07-07-ai-customer-service-design.md §4.3
-- =============================================================

CREATE TABLE IF NOT EXISTS knowledge_doc (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id     BIGINT       NOT NULL                COMMENT '租户ID',
    source_type   VARCHAR(32)  NOT NULL DEFAULT 'UPLOAD' COMMENT 'UPLOAD/SYNC_PRODUCT/SYNC_POLICY/SYNC_FAQ',
    source_id     VARCHAR(128)                         COMMENT '业务方ID（同步用）',
    title         VARCHAR(255) NOT NULL                COMMENT '文档标题',
    file_name     VARCHAR(255)                         COMMENT '文件名（UPLOAD）',
    content_type  VARCHAR(128)                         COMMENT 'MIME',
    content_hash  VARCHAR(64)  NOT NULL                COMMENT '内容SHA-256（幂等）',
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/INDEXING/READY/FAILED',
    error_message VARCHAR(1024)                        COMMENT '失败原因',
    chunk_count   INT          NOT NULL DEFAULT 0     COMMENT '分段数',
    char_count    INT          NOT NULL DEFAULT 0     COMMENT '字符数',
    create_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_tenant_status (tenant_id, status),
    KEY idx_tenant_hash (tenant_id, content_hash),
    KEY idx_tenant_source (tenant_id, source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档';

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       BIGINT       NOT NULL                COMMENT '租户ID',
    doc_id          BIGINT       NOT NULL                COMMENT '所属文档ID',
    chunk_index     INT          NOT NULL                COMMENT '分段顺序（0-based）',
    content         TEXT         NOT NULL                COMMENT '分段文本',
    embedding_model VARCHAR(64)  NOT NULL                COMMENT 'Embedding 模型名',
    vector_ref      BIGINT       NOT NULL                COMMENT '向量库内部ID（InMemory 自增）',
    token_count     INT                                   COMMENT '该段 token 数',
    create_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_doc_index (tenant_id, doc_id, chunk_index),
    KEY idx_tenant_doc (tenant_id, doc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库分段';
