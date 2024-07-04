package com.kato.pro.rec.service.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.RateLimiter;
import com.kato.pro.common.constant.CommonCode;
import com.kato.pro.common.exception.KatoServiceException;
import com.kato.pro.rec.entity.constant.AbOrNacosConstant;
import com.kato.pro.rec.entity.enums.LimiterCategory;
import com.kato.pro.base.service.NacosPropertyAcquirer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@SuppressWarnings("ALL")
public class RateGateway implements CommandLineRunner {

    @Resource
    NacosPropertyAcquirer nacosPropertyAcquirer;

    private Map<String, RateLimiter> limiterMap;

    @Override
    public void run(String... args) throws Exception {
        init();
    }

    public void init() {
        String permitsPerSecond = Optional.ofNullable(nacosPropertyAcquirer.getProperty(AbOrNacosConstant.RECOMMEND_API_RATE_LIMIT)).orElse("100");
        RateLimiter recommendRateLimiter = RateLimiter.create(Integer.parseInt(permitsPerSecond));
        limiterMap = ImmutableMap.of(LimiterCategory.RECOMMEND.lowerName(), recommendRateLimiter);
    }

    /**
     * pretreatment: check param & acquire
     */
    public void tryAcquire(LimiterCategory category) {
        if (category == null) {
            return;
        }
        RateLimiter rateLimiter = limiterMap.get(category.lowerName());
        if (rateLimiter == null) {
            return;
        }
        if (!rateLimiter.tryAcquire()) {
            throw new KatoServiceException(CommonCode.REQUEST_RATE_LIMIT);
        }
    }

}
