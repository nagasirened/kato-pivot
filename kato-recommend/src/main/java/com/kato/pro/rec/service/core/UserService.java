package com.kato.pro.rec.service.core;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.kato.pro.base.log.ScaleLogger;
import com.kato.pro.base.util.ConfigUtils;
import com.kato.pro.common.entity.LevelEnum;
import com.kato.pro.common.entity.LoginUser;
import com.kato.pro.common.resolver.DeviceContextHolder;
import com.kato.pro.common.resolver.LoginUserContextHolder;
import com.kato.pro.common.utils.DateHelper;
import com.kato.pro.rec.entity.constant.AbOrNacosConstant;
import com.kato.pro.rec.entity.constant.LogConstant;
import com.kato.pro.rec.utilities.RedisKey;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

@Service
public class UserService {

    @Resource private CacheService cacheService;

    /**
     * 判断当前是不是新用户，true代表新用户，false代表老用户
     * userId不存在的，用deviceId查询，都不存在默认新用户
     */
    public Boolean isNewUser(Map<String, String> abMap) {
        LoginUser loginUser = LoginUserContextHolder.getLoginUser();
        String uniqueKey = CharSequenceUtil.isBlank(loginUser.getUserId()) ? "deviceId" : loginUser.getUserId();
        if (CharSequenceUtil.isBlank(uniqueKey)) {
            return true;
        }
        String redisKey = RedisKey.USER_SIGN_IN_DATE.makeRedisKey(uniqueKey);
        String registration = cacheService.get(redisKey);
        if (CharSequenceUtil.isBlank(registration)) {
            return true;
        }
        int days = Convert.toInt(ConfigUtils.getAbOrProperty(abMap, AbOrNacosConstant.DEFINE_NEW_USER_DAYS, "1"));
        return DateUtil.between(DateUtil.parse(registration), DateUtil.date(), DateUnit.DAY) + 1 <= days;
    }

    /**
     * 当天召回的次数，可能有用. 没有打开开关的话默认为 -1
     */
    private int refreshObtainNumber(Map<String, String> abMap) {
        int res = -1;
        try {
            boolean verifySwitch = ConfigUtils.checkRule(abMap, AbOrNacosConstant.OBTAIN_NUMBER);
            if (!verifySwitch) return res;

            String date = DateHelper.currentMMdd();
            String redisKey = RedisKey.RETRIEVE_DEVICE_OBTAIN_NUMBER.makeRedisKey(DeviceContextHolder.getDeviceId(), date);
            return Convert.toInt(cacheService.incr(redisKey));
        } finally {
            ScaleLogger.putLog(LogConstant.OBTAIN_NUMBER, -1, LevelEnum.NORMAL);
        }
    }

}
