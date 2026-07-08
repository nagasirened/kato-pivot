-- 风控系统数据库初始化脚本

CREATE DATABASE IF NOT EXISTS kato_risk DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE kato_risk;

-- 风控规则表
CREATE TABLE IF NOT EXISTS risk_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL COMMENT '规则名称',
    scene VARCHAR(32) NOT NULL COMMENT '环节：REGISTER/LOGIN/MARKETING/ORDER/PAYMENT/REFUND',
    rule_type VARCHAR(16) NOT NULL COMMENT '规则类型：CONFIG/GROOVY',
    content TEXT NOT NULL COMMENT '规则内容（SpEL表达式或Groovy脚本）',
    priority INT DEFAULT 99 COMMENT '优先级，数字越小越优先',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    version INT DEFAULT 1 COMMENT '版本号，每次编辑+1',
    ab_group VARCHAR(16) COMMENT 'A/B实验分组，如10%',
    created_by VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_scene (scene),
    INDEX idx_enabled (enabled),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控规则表';

-- 风控案件表
CREATE TABLE IF NOT EXISTS risk_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) COMMENT '用户ID',
    scene VARCHAR(32) NOT NULL COMMENT '环节',
    request_id VARCHAR(128) COMMENT '请求唯一ID',
    action VARCHAR(16) NOT NULL COMMENT '决策：PASS/REVIEW/BLOCK',
    risk_score DECIMAL(5,4) DEFAULT 0 COMMENT '风险分0.0~1.0',
    reason_codes VARCHAR(512) COMMENT '命中的规则编码列表',
    request_context JSON COMMENT '请求上下文JSON',
    status VARCHAR(16) DEFAULT 'PENDING' COMMENT '状态：PENDING/HANDLED/REJECTED/REVOKED',
    handler VARCHAR(64) COMMENT '处理人',
    handle_note VARCHAR(512) COMMENT '处理备注',
    handle_time DATETIME COMMENT '处理时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_scene (scene),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at),
    INDEX idx_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控案件表';

-- 规则变更审计日志表
CREATE TABLE IF NOT EXISTS risk_rule_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL COMMENT '规则ID',
    operation VARCHAR(16) NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/ENABLE/DISABLE',
    operator VARCHAR(64) DEFAULT 'system' COMMENT '操作人',
    old_content TEXT COMMENT '变更前内容',
    new_content TEXT COMMENT '变更后内容',
    operate_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_rule_id (rule_id),
    INDEX idx_operate_at (operate_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则变更审计日志表';

-- 拦截日志表
CREATE TABLE IF NOT EXISTS risk_reject_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(128) COMMENT '请求唯一ID',
    user_id VARCHAR(64) COMMENT '用户ID',
    scene VARCHAR(32) COMMENT '环节',
    action VARCHAR(16) COMMENT '决策',
    risk_score DECIMAL(5,4) COMMENT '风险分',
    reason_codes VARCHAR(512) COMMENT '规则编码',
    hit_rules VARCHAR(512) COMMENT '命中规则列表',
    request_context JSON COMMENT '请求上下文',
    rule_type VARCHAR(16) COMMENT '规则类型',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_request_id (request_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拦截日志表';

-- 场景配置表
CREATE TABLE IF NOT EXISTS risk_scene_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scene VARCHAR(32) NOT NULL UNIQUE COMMENT '场景编码',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    default_action VARCHAR(16) DEFAULT 'PASS' COMMENT '默认放行动作',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_scene (scene)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场景配置表';

-- 初始化场景配置
INSERT INTO risk_scene_config (scene, enabled, default_action) VALUES
('REGISTER', 1, 'PASS'),
('LOGIN', 1, 'PASS'),
('MARKETING', 1, 'PASS'),
('ORDER', 1, 'PASS'),
('PAYMENT', 1, 'PASS'),
('REFUND', 1, 'PASS')
ON DUPLICATE KEY UPDATE scene=scene;

-- 示例规则
INSERT INTO risk_rule (name, scene, rule_type, content, priority, enabled, created_by) VALUES
('测试用户拦截', 'REGISTER', 'GROOVY', '// ctx: userId, deviceId, ip, scene\ndef action = "PASS"\ndef score = 0.0\nif (ctx.userId && ctx.userId.startsWith("test")) {\n    action = "BLOCK"\n    score = 0.8\n}\nreturn [action: action, score: score]', 1, 1, 'system'),
('IP黑名单拦截', 'LOGIN', 'CONFIG', '#ip != null && ip.startsWith("192.168.")', 2, 1, 'system');