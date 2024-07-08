package com.kato.pro.base.util;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.util.concurrent.RateLimiter;
import com.kato.pro.common.constant.BaseConstant;
import com.kato.pro.common.constant.CommonCode;
import com.kato.pro.common.exception.KatoServiceException;
import com.kato.pro.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 接口限速类
 *
 * 使用方式：项目接口配置api.rate.limit={"recommend": 100}  前面是key,后面的是每秒的限制
 * 代码中使用RateGateway.tryAcquire(${key})即可
 */
@Slf4j
@Component
@SuppressWarnings("ALL")
public class RateGateway implements CommandLineRunner {

    private static Map<String, RateLimiter> limiterMap;

    @Override
    public void run(String... args) throws Exception {
        init();
    }

    @Scheduled(cron = "50 0/10 * * * ?")
    public void init() {
        String permitsPerSecond = ConfigUtils.getProperty(BaseConstant.RECOMMEND_API_RATE_LIMIT, "{}");
        Map<String, Integer> map = JsonUtils.toMap(permitsPerSecond, String.class, Integer.class);
        if (MapUtil.isNotEmpty(map)) {
            map.forEach((k, v) -> limiterMap.put(k, RateLimiter.create(v)));
        }
    }

    /**
     * pretreatment: check param & acquire
     */
    public static void tryAcquire(String categoryName) {
        if (StrUtil.isBlank(categoryName)) {
            return;
        }
        RateLimiter rateLimiter = limiterMap.get(categoryName);
        if (rateLimiter == null) {
            return;
        }
        if (!rateLimiter.tryAcquire()) {
            throw new KatoServiceException(CommonCode.REQUEST_RATE_LIMIT);
        }
    }

}
