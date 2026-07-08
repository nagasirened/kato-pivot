-- ============================================================
-- 风控系统 MySQL 建表脚本
-- 数据库: kato_risk
-- 字符集: utf8mb4 / utf8mb4_general_ci
-- 用途: 与 kato-risk-server 实体类(MyBatis-Plus)严格对齐
-- 实体来源:
--   com.kato.pro.risk.server.entity.RiskRule
--   com.kato.pro.risk.server.entity.RiskCase
--   com.kato.pro.risk.server.entity.RiskRejectLog
--   com.kato.pro.risk.server.entity.RiskRuleAudit
-- 客户端(kato-risk-client)为纯 SDK, 不涉及数据库表
-- ============================================================

CREATE DATABASE IF NOT EXISTS kato_risk DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE kato_risk;

-- --------------------------------------------------------
-- 1. 风控规则表 risk_rule
-- 对应实体: RiskRule
-- 备注: content 字段存 SpEL 表达式 (CONFIG) 或 Groovy 脚本 (GROOVY)
--       version 使用 MyBatis-Plus @Version 乐观锁
-- --------------------------------------------------------
DROP TABLE IF EXISTS risk_rule;
CREATE TABLE risk_rule (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    name            VARCHAR(128) NOT NULL                COMMENT '规则名称',
    scene           VARCHAR(32)  NOT NULL                COMMENT '适用环节: REGISTER/LOGIN/MARKETING/ORDER/PAYMENT/REFUND',
    rule_type       VARCHAR(16)  NOT NULL                COMMENT '规则类型: CONFIG/GROOVY',
    content         TEXT         NOT NULL                COMMENT '规则内容: SpEL 表达式 或 Groovy 脚本',
    priority        INT          NOT NULL DEFAULT 99     COMMENT '优先级, 数字越小越优先执行',
    enabled         TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '是否启用: 0-禁用 1-启用',
    version         INT          NOT NULL DEFAULT 1      COMMENT '版本号, 乐观锁, 每次更新 +1',
    ab_group        VARCHAR(32)  DEFAULT NULL            COMMENT 'A/B 实验分组, 如 A/B/NULL',
    created_by      VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP             COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_scene_enabled_priority (scene, enabled, priority),
    KEY idx_ab_group (ab_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控规则表';

-- --------------------------------------------------------
-- 2. 规则变更审计表 risk_rule_audit
-- 对应实体: RiskRuleAudit
-- 备注: 记录规则的 CREATE/UPDATE/DELETE/ENABLE/DISABLE
-- --------------------------------------------------------
DROP TABLE IF EXISTS risk_rule_audit;
CREATE TABLE risk_rule_audit (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '审计ID',
    rule_id         BIGINT       NOT NULL                COMMENT '规则ID',
    operation       VARCHAR(16)  NOT NULL                COMMENT '操作类型: CREATE/UPDATE/DELETE/ENABLE/DISABLE',
    operator        VARCHAR(64)  NOT NULL                COMMENT '操作人',
    old_content     TEXT         DEFAULT NULL            COMMENT '变更前规则内容',
    new_content     TEXT         DEFAULT NULL            COMMENT '变更后规则内容',
    operate_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_rule_id (rule_id),
    KEY idx_operator_time (operator, operate_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则变更审计表';

-- --------------------------------------------------------
-- 3. 风控案件表 risk_case
-- 对应实体: RiskCase
-- 备注: 每次风控请求落库(供审核/复盘/统计)
-- --------------------------------------------------------
DROP TABLE IF EXISTS risk_case;
CREATE TABLE risk_case (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '案件ID',
    user_id         VARCHAR(64)   DEFAULT NULL            COMMENT '用户ID',
    scene           VARCHAR(32)   NOT NULL                COMMENT '环节',
    request_id      VARCHAR(64)   NOT NULL                COMMENT '请求唯一ID',
    action          VARCHAR(16)   NOT NULL                COMMENT '决策: PASS/REVIEW/BLOCK',
    risk_score      DECIMAL(5,2)  DEFAULT NULL            COMMENT '风险分 0.00~1.00',
    reason_codes    VARCHAR(512)  DEFAULT NULL            COMMENT '命中规则编码列表, 逗号分隔',
    request_context TEXT          DEFAULT NULL            COMMENT '请求上下文 JSON',
    status          VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT '案件状态: PENDING/HANDLED/REJECTED/REVOKED',
    handler         VARCHAR(64)   DEFAULT NULL            COMMENT '处理人',
    handle_note     VARCHAR(1024) DEFAULT NULL            COMMENT '处理备注',
    handle_time     DATETIME      DEFAULT NULL            COMMENT '处理时间',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uniq_request_id (request_id),
    KEY idx_user_time (user_id, created_at),
    KEY idx_scene_time (scene, created_at),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控案件表';

-- --------------------------------------------------------
-- 4. 风控拒绝日志表 risk_reject_log
-- 对应实体: RiskRejectLog
-- 备注: 仅在 BLOCK 决策时写入, 用于事后追溯与规则调优
-- --------------------------------------------------------
DROP TABLE IF EXISTS risk_reject_log;
CREATE TABLE risk_reject_log (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    request_id      VARCHAR(64)   NOT NULL                COMMENT '请求ID',
    user_id         VARCHAR(64)   DEFAULT NULL            COMMENT '用户ID',
    scene           VARCHAR(32)   NOT NULL                COMMENT '环节',
    action          VARCHAR(16)   NOT NULL                COMMENT '决策动作',
    risk_score      DECIMAL(5,2)  DEFAULT NULL            COMMENT '风险得分',
    hit_rules       TEXT          DEFAULT NULL            COMMENT '命中规则列表 JSON',
    reason_codes    VARCHAR(512)  DEFAULT NULL            COMMENT '命中规则编码',
    request_context TEXT          DEFAULT NULL            COMMENT '请求上下文 JSON',
    rule_type       VARCHAR(16)   DEFAULT 'MIXED'         COMMENT '规则类型: CONFIG/GROOVY/MIXED',
    script_version  VARCHAR(32)   DEFAULT NULL            COMMENT 'Groovy 脚本版本',
    ab_group        VARCHAR(32)   DEFAULT NULL            COMMENT 'A/B 实验分组',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (id),
    UNIQUE KEY uniq_request_id (request_id),
    KEY idx_user_time (user_id, created_at),
    KEY idx_scene_time (scene, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控拒绝日志表';

-- ============================================================
-- Phase 1 示例数据: REGISTER 环节规则 (验证规则引擎可用性)
-- 数据与 RiskRule 实体严格对齐
-- ============================================================

INSERT INTO risk_rule (name, scene, rule_type, content, priority, enabled, version, created_by) VALUES
-- REGISTER 场景配置规则1: IP 段白名单/黑名单检测 (SpEL)
('IP内网段注册',     'REGISTER', 'CONFIG', '#ip != null and (#ip.startsWith("192.168.") or #ip.startsWith("10.0."))', 1, 1, 1, 'system'),
-- REGISTER 场景配置规则2: 设备 ID 异常检测
('设备ID异常',       'REGISTER', 'CONFIG', '#deviceId == null or #deviceId.length() < 8',                    2, 1, 1, 'system'),
-- REGISTER 场景配置规则3: 疑似机器人注册 (用户名特征)
('用户名机器人检测', 'REGISTER', 'CONFIG', '#userId != null and (#userId matches "^test\\d+$" or #userId matches "^bot\\d+$")', 3, 1, 1, 'system');

-- REGISTER 场景 Groovy 动态规则示例 (AST 安全审核后入库)
INSERT INTO risk_rule (name, scene, rule_type, content, priority, enabled, version, created_by) VALUES
('新用户IP段风险评估', 'REGISTER', 'GROOVY',
'import groovy.json.JsonSlurper
def ctx = binding.variables.get("ctx")
def userId = ctx.userId
def ip = ctx.ip
def result = [:]
result.action = "PASS"
result.score = 0.0
result.reasonCode = ""
if (ip != null && (ip.startsWith("1.1.") || ip.startsWith("2.2."))) {
    result.action = "REVIEW"
    result.score = 0.5
    result.reasonCode = "NEW_USER_SUSPICIOUS_IP"
}
result', 4, 1, 1, 'system');
