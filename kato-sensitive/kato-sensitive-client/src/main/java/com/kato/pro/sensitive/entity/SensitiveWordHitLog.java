package com.kato.pro.sensitive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 敏感词命中日志
 */
@Data
@TableName("sensitive_word_hit_log")
public class SensitiveWordHitLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 检测文本长度
     */
    private Integer textLength;

    /**
     * 命中数量
     */
    private Integer hitCount;

    /**
     * 命中等级列表（逗号分隔）
     */
    private String hitLevels;

    /**
     * 命中的敏感词（逗号分隔）
     */
    private String hitWords;

    /**
     * 命中分类列表（逗号分隔）
     */
    private String hitCategorys;

    /**
     * 检测时间
     */
    private LocalDateTime checkTime;

    /**
     * 检测类型：EXACT/REGEX/FUZZY
     */
    private String checkType;

    /**
     * 响应时间（毫秒）
     */
    private Integer responseTime;

    /**
     * 客户端IP
     */
    private String clientIp;
}
