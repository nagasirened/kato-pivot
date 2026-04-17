package com.kato.pro.sensitive.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kato.pro.sensitive.entity.SensitiveOperationLog;

/**
 * 敏感词操作日志服务接口
 */
public interface ISensitiveOperationLogService {

    /**
     * 分页查询操作日志
     */
    IPage<SensitiveOperationLog> pageLogs(Page<SensitiveOperationLog> page, SensitiveOperationLog query);

    /**
     * 保存操作日志
     */
    void saveLog(SensitiveOperationLog log);

    /**
     * 保存操作日志（简洁方法）
     */
    void saveLog(String operationType, Long wordId, String wordContent,
                 Integer operatorId, String operatorIp,
                 Object beforeData, Object afterData);
}
