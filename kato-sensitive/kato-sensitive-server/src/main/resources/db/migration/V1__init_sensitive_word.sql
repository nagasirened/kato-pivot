-- 敏感词表
CREATE TABLE IF NOT EXISTS `sensitive_word` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `word` VARCHAR(100) NOT NULL COMMENT '敏感词内容',
    `level` VARCHAR(20) NOT NULL COMMENT '敏感等级：URGENT-紧急, MEDIUM-中等, NORMAL-普通',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLE' COMMENT '状态：ENABLE-启用, DISABLE-禁用',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator_id` INT DEFAULT NULL COMMENT '创建人ID',
    PRIMARY KEY (`id`),
    INDEX `idx_word` (`word`),
    INDEX `idx_level` (`level`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';
