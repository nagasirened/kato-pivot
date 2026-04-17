package com.kato.pro.sensitive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 敏感词操作日志
 */
@Data
@TableName("sensitive_operation_log")
public class SensitiveOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作类型：CREATE/UPDATE/DELETE/IMPORT/EXPORT
     */
    private String operationType;

    /**
     * 关联敏感词ID
     */
    private Long wordId;

    /**
     * 敏感词内容
     */
    private String wordContent;

    /**
     * 操作人ID
     */
    private Integer operatorId;

    /**
     * 操作人IP
     */
    private String operatorIp;

    /**
     * 操作前数据(JSON)
     */
    private String beforeData;

    /**
     * 操作后数据(JSON)
     */
    private String afterData;

    /**
     * 创建时间
     */
    private Date createTime;
}
