package com.kato.pro.rec.utilities;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kato.pro.base.constant.CommonConstant;
import com.kato.pro.base.service.NacosPropertyAcquirer;
import com.kato.pro.rec.entity.constant.AbParamConstant;
import com.kato.pro.rec.entity.enums.LevelEnum;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class LogsDetailUtils {

    /**
     * 所有人默认日志等级是1，仅被配置的部分用户等级可以是2或者3
     */
    @Resource
    NacosPropertyAcquirer nacosPropertyAcquirer;

    public static final String LOG_ON_OFF = "logOnOff";
    private Integer interval = 10000;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final LogSupport logSupport = new LogSupport();

    @PostConstruct
    public void loadInterval() {
        String intervalProperty = nacosPropertyAcquirer.getProperty(AbParamConstant.LOG_DETAIL_INTERVAL, "10000");
        try {
            Integer logInterval = Convert.toInt(intervalProperty);
            if (logInterval > 0) {
                log.info("LogDetailUtils#loadInterval, logDetail's interval param in nacos is {}, load success", intervalProperty);
                interval = logInterval;
            }
        } catch (NumberFormatException e) {
            log.error("LogDetailUtils#loadInterval, logDetail's interval param in nacos is wrong, info: {}", intervalProperty, e);
        }
    }

    /**
     * 验证并初始化日志等级
     * @param deviceId      设备号
     */
    public void init(String deviceId) {
        int level = userStrongHit(deviceId);
        boolean flag = false;
        if (level > LevelEnum.DETAIL.getLevel()) {
            flag = true;
            initSupport(deviceId, level);
        }
        if (flag) return;

        int cnt = counter.incrementAndGet();
        if (cnt % interval == 0) {
            counter.getAndUpdate((prev) -> 0);
            initSupport(deviceId, level);
            return;
        }
        MDC.put(LOG_ON_OFF, "0");
    }

    /**
     * 如果要记录日志，实例化专项Map, 并打开当前请求的日志开关LOG_ON_OFF
     * @param deviceId  设备号
     * @param level     等级
     */
    private void initSupport(String deviceId, Integer level) {
        logSupport.initMap();
        logSupport.setLevel(level);
        putLog("deviceId", deviceId, LevelEnum.NORMAL);
        MDC.put(LOG_ON_OFF, "1");
    }

    /**
     * 强命中用户处理
     * @param deviceId  设备号
     * @return  level
     */
    private int userStrongHit(String deviceId) {
        try {
            String strongHitStr = nacosPropertyAcquirer.getProperty(AbParamConstant.LOG_DETAIL_STRONG_HIT, "{}");
            JSONObject userLevelDictionary = JSONObject.parseObject(strongHitStr);
            if (userLevelDictionary.containsKey(deviceId)) {
                return userLevelDictionary.getInteger(deviceId);
            }
        } catch (Exception e) {
            log.error("LogDetailUtils#userStrongHit, the config in nacos about userStrongHit is wrong, deviceId: {}", deviceId, e);
        }
        return 1;
    }

    /**
     * 实际记录日志
     */
    public void windUp() {
        if (StrUtil.equals("1", MDC.get(LOG_ON_OFF))) {
            log.info("LogsDetailUtils#windUp, currentMapSize: {}, logsInfo: {}", logSupport.getActiveSize(), JSON.toJSONString(logSupport.getTraceMap()));
        }
        logSupport.reset();
        MDC.remove(LOG_ON_OFF);
    }

    /**
     * 添加日志信息
     */
    public void putLog(String key, Object value, LevelEnum levelEnum) {
        if (!logSupport.verify(levelEnum.getLevel())) {
            return;
        }
        logSupport.put(key, value);
    }

    private static class LogSupport {

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
                detailMap = logMap.get(getTraceId());
            }
            detailMap.put(key, value);
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

}
