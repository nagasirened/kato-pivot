-- 敏感词表新增分类字段
ALTER TABLE sensitive_word ADD COLUMN category VARCHAR(50) DEFAULT 'OTHER' COMMENT '分类：POLITICS-政治, PORN-色情, AD-广告, VIOLENCE-暴恐, FRAUD-诈骗, OTHER-其他';

-- 操作日志表
CREATE TABLE IF NOT EXISTS `sensitive_operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `operation_type` VARCHAR(20) NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/IMPORT/EXPORT/ENABLE/DISABLE',
    `word_id` BIGINT DEFAULT NULL COMMENT '关联敏感词ID',
    `word_content` VARCHAR(100) DEFAULT NULL COMMENT '敏感词内容',
    `operator_id` INT DEFAULT NULL COMMENT '操作人ID',
    `operator_ip` VARCHAR(50) DEFAULT NULL COMMENT '操作人IP',
    `before_data` TEXT COMMENT '操作前数据',
    `after_data` TEXT COMMENT '操作后数据',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_word_id` (`word_id`),
    INDEX `idx_operation_type` (`operation_type`),
    INDEX `idx_operator_id` (`operator_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词操作日志表';
