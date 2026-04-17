package com.kato.pro.sensitive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kato.pro.base.entity.SuperModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 敏感词实体
 */
@Data
@EqualsAndHashCode(of = "id", callSuper = false)
@TableName("sensitive_word")
public class SensitiveWord extends SuperModel<SensitiveWord> {

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
     * 备注
     */
    private String remark;

    /**
     * 分类：POLITICS-政治, PORN-色情, AD-广告, VIOLENCE-暴恐, FRAUD-诈骗, OTHER-其他
     */
    private String category;
}
