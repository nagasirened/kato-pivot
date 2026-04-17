package com.kato.pro.sensitive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 敏感词实体（客户端使用）
 */
@Data
@TableName("sensitive_word")
public class SensitiveWord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 敏感词内容
     */
    private String word;

    /**
     * 敏感等级：URGENT-紧急, MEDIUM-中等, NORMAL-普通
     */
    private String level;

    /**
     * 状态：ENABLE-启用, DISABLE-禁用
     */
    private String status;

    /**
     * 分类
     */
    private String category;

    /**
     * 敏感词类型：EXACT-精确匹配, REGEX-正则匹配
     */
    private String wordType;

    /**
     * 备注
     */
    private String remark;
}
