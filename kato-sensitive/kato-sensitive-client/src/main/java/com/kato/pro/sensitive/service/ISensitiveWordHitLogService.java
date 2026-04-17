package com.kato.pro.sensitive.service;

import com.kato.pro.sensitive.dto.SensitiveCheckRequest;
import com.kato.pro.sensitive.dto.SensitiveCheckResult;

/**
 * 敏感词命中日志服务接口
 */
public interface ISensitiveWordHitLogService {

    /**
     * 异步记录命中日志
     */
    void recordHit(SensitiveCheckRequest request, SensitiveCheckResult result);

    /**
     * 同步记录命中日志
     */
    void recordHitSync(SensitiveCheckRequest request, SensitiveCheckResult result);
}
