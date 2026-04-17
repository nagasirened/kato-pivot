package com.kato.pro.sensitive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.sensitive.entity.SensitiveOperationLog;
import com.kato.pro.sensitive.mapper.SensitiveOperationLogMapper;
import com.kato.pro.sensitive.service.ISensitiveOperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 敏感词操作日志服务实现
 */
@Slf4j
@Service
public class SensitiveOperationLogServiceImpl implements ISensitiveOperationLogService {

    @Resource
    private SensitiveOperationLogMapper operationLogMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public IPage<SensitiveOperationLog> pageLogs(Page<SensitiveOperationLog> page, SensitiveOperationLog query) {
        LambdaQueryWrapper<SensitiveOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getWordId() != null) {
                wrapper.eq(SensitiveOperationLog::getWordId, query.getWordId());
            }
            if (query.getOperationType() != null && !query.getOperationType().isEmpty()) {
                wrapper.eq(SensitiveOperationLog::getOperationType, query.getOperationType());
            }
            if (query.getOperatorId() != null) {
                wrapper.eq(SensitiveOperationLog::getOperatorId, query.getOperatorId());
            }
        }
        wrapper.orderByDesc(SensitiveOperationLog::getCreateTime);
        return operationLogMapper.selectPage(page, wrapper);
    }

    @Override
    @Async
    public void saveLog(SensitiveOperationLog operationLog) {
        try {
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

    @Override
    @Async
    public void saveLog(String operationType, Long wordId, String wordContent,
                         Integer operatorId, String operatorIp,
                         Object beforeData, Object afterData) {
        try {
            SensitiveOperationLog log = new SensitiveOperationLog();
            log.setOperationType(operationType);
            log.setWordId(wordId);
            log.setWordContent(wordContent);
            log.setOperatorId(operatorId);
            log.setOperatorIp(operatorIp);
            log.setBeforeData(toJson(beforeData));
            log.setAfterData(toJson(afterData));
            log.setCreateTime(new Date());
            operationLogMapper.insert(log);
        } catch (Exception e) {
            SensitiveOperationLogServiceImpl.log.error("保存操作日志失败", e);
        }
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }
}
