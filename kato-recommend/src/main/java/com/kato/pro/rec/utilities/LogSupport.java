package com.kato.pro.rec.utilities;

import com.kato.pro.rec.entity.constant.CommonConstant;
import com.kato.pro.rec.entity.enums.LevelEnum;
import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分级日志支持器
 */
public class LogSupport {

    private final Map<String, LinkedHashMap<String, Object>> logMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> logLevel = new ConcurrentHashMap<>();

    /** 唯一标记 */
    private String getTraceId() {
        return MDC.get(CommonConstant.TRACE_ID);
    }

    /** 初始化 */
    public void initMap() {
        logMap.put(getTraceId(), new LinkedHashMap<>());
    }

    public void put(String key, Object value) {
        LinkedHashMap<String, Object> detailMap = logMap.get(getTraceId());
        if (detailMap == null) {
            initMap();
        }
        put(key, value);
    }

    public Map<String, Object> getTraceMap() {
        return logMap.get(getTraceId());
    }

    public void reset() {
        logMap.remove(getTraceId());
    }

    public Integer getActiveSize() {
        return logMap.size();
    }

    /**
     * 日志等级相关函数
     */
    public void setDefaultLevel() {
        logLevel.put(getTraceId(), LevelEnum.NORMAL.getLevel());
    }

    public void setLevel(LevelEnum levelEnum) {
        logLevel.put(getTraceId(), levelEnum.getLevel());
    }

    public void setLevel(Integer level) {
        logLevel.put(getTraceId(), level);
    }

    public Integer getLevel() {
        return logLevel.get(getTraceId());
    }

    public Boolean verify(Integer currentLevel) {
        return getLevel() >= currentLevel;
    }

}
