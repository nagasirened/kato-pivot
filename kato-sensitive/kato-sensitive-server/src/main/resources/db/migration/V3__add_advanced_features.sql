-- 敏感词表新增类型字段
ALTER TABLE sensitive_word ADD COLUMN word_type VARCHAR(20) DEFAULT 'EXACT' COMMENT '类型：EXACT-精确, REGEX-正则';

-- 命中日志表
CREATE TABLE sensitive_word_hit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    text_length INT COMMENT '检测文本长度',
    hit_count INT COMMENT '命中数量',
    hit_levels VARCHAR(100) COMMENT '命中等级列表',
    hit_words TEXT COMMENT '命中的敏感词',
    hit_categorys VARCHAR(200) COMMENT '命中分类列表',
    check_time DATETIME COMMENT '检测时间',
    check_type VARCHAR(20) COMMENT '检测类型：EXACT/REGEX/FUZZY',
    response_time INT COMMENT '响应时间(毫秒)',
    client_ip VARCHAR(50) COMMENT '客户端IP',
    INDEX idx_check_time (check_time),
    INDEX idx_hit_levels (hit_levels)
);

-- 敏感词审核表
CREATE TABLE sensitive_word_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(100) NOT NULL COMMENT '敏感词',
    level VARCHAR(20) COMMENT '敏感等级',
    category VARCHAR(50) COMMENT '分类',
    status VARCHAR(20) COMMENT '状态',
    remark VARCHAR(500) COMMENT '备注',
    audit_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '审核状态：PENDING/APPROVED/REJECTED',
    applicant_id INT COMMENT '申请人ID',
    auditor_id INT COMMENT '审核人ID',
    audit_time DATETIME COMMENT '审核时间',
    audit_remark VARCHAR(500) COMMENT '审核备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_audit_status (audit_status),
    INDEX idx_applicant (applicant_id)
);
