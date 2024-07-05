package com.kato.pro.base.log;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kato.pro.base.util.ConfigUtils;
import com.kato.pro.common.constant.PropertyConstant;
import com.kato.pro.common.constant.BaseConstant;
import com.kato.pro.common.entity.LevelEnum;
import com.kato.pro.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 日志抽样工具类
 * 设置interval，用户默认等级1，每interval次数次调用打印一次用户当前的日志信息。 开发者埋点日志内容
 * 等级2及以上, 每条日志都打印，但是等级不一样，有些日志可以设置至少是某个等级的才能收集，低于某个等级的不搜集日志. >3的按3算
 */
@Slf4j
public class ScaleLogger {

    // 开关key
    public static final String LOG_SWITCH = "logSwitch";
    // 计数器
    private static final AtomicInteger counter = new AtomicInteger(0);
    // 辅助器
    private static final LogSupport logSupport = new LogSupport();

    /**
     * 验证并初始化日志等级
     * @param deviceId      设备号
     */
    public static void init(String deviceId) {
        int level = userStrongHit(deviceId);
        // level >= 2, 默认开启
        if (level >= LevelEnum.MIDDLE.getLevel()) {
            initSupport(deviceId, level);
            return;
        }
        // level = 1
        int cnt = counter.incrementAndGet();
        Integer interval = Optional.ofNullable(ConfigUtils.getIntegerProperty(PropertyConstant.LOG_INTERVAL)).orElse(10000);
        if (cnt % interval == 0) {
            counter.getAndUpdate((prev) -> 0);  // 重置为0
            initSupport(deviceId, level);
            return;
        }
        // 不加入日志
        MDC.put(LOG_SWITCH, BaseConstant.OFF);
    }

    private static boolean logSwitchIsON() {
        return BaseConstant.ON.equals(MDC.get(LOG_SWITCH));
    }

    /**
     * 如果要记录日志，实例化专项Map, 并打开当前请求的日志开关LOG_ON_OFF
     * @param deviceId  设备号
     * @param level     等级
     */
    private static void initSupport(String deviceId, Integer level) {
        // 打开容器
        logSupport.initialContainer(level);
        putLog(BaseConstant.DEVICE_ID, deviceId, LevelEnum.NORMAL);
        MDC.put(LOG_SWITCH, BaseConstant.ON);
    }

    /**
     * 强命中用户处理
     * @param deviceId  设备号
     * @return  level
     */
    private static int userStrongHit(String deviceId) {
        String strongHits = ConfigUtils.getProperty(PropertyConstant.LOG_STRONG_HITS, "{}");
        if (StrUtil.isBlank(strongHits) || "{}".equals(strongHits)) return 1;
        try {
            // 结合@RefreshScope，保证配置文件更新后，实时生效
            Map<String, String> userLevelDictionary = JsonUtils.toObject(strongHits, new TypeReference<Map<String, String>>(){} );
            if (MapUtil.isNotEmpty(userLevelDictionary)) {
                String val = Optional.ofNullable(userLevelDictionary.get(deviceId)).orElse("1");
                return Convert.toInt(val);
            }
        } catch (Exception e) {
            log.error("LogDetailUtils#userStrongHit, the config in configuration about userStrongHit is wrong, deviceId: {}", deviceId, e);
        }
        return 1;
    }

    /**
     * 实际记录日志
     */
    public static void windUp() {
        try {
            if (logSwitchIsON()) {
                log.info("LogsDetailUtils#windUp, currentMapSize: {}, logsInfo: {}",
                        logSupport.getActiveSize(), JsonUtils.toJSONString(logSupport.getTraceMap()));
            }
        } finally {
            logSupport.reset();
            MDC.remove(LOG_SWITCH);
        }
    }

    /**
     * 添加日志信息
     */
    public static void putLog(String key, Object value, LevelEnum levelEnum) {
        if (!logSwitchIsON() || !logSupport.verify(levelEnum.getLevel())) return;
        logSupport.put(key, value);
    }

    /**
     * 日志信息、等级存储器
     */
    private static class LogSupport {
        private final Map<String, LinkedHashMap<String, Object>> logMap = new ConcurrentHashMap<>();
        private final Map<String, Integer> logLevel = new ConcurrentHashMap<>();

        /** 唯一标记 */
        private String getTraceId() {
            return MDC.get(BaseConstant.TRACE_ID);
        }

        /** 初始化 */
        public void initialContainer(int level) {
            String traceId = getTraceId();
            logMap.put(traceId, new LinkedHashMap<>());
            logLevel.put(traceId, level);
        }

        public void put(String key, Object value) {
            LinkedHashMap<String, Object> detailMap = logMap.get(getTraceId());
            if (detailMap == null) {
                initialContainer(1);
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

        public Integer getLevel() {
            return logLevel.get(getTraceId());
        }

        public Boolean verify(Integer currentLevel) {
            return getLevel() >= currentLevel;
        }

    }

}
