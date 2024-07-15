package com.kato.pro.rec.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.kato.pro.base.util.ConfigUtils;
import com.kato.pro.common.constant.BaseConstant;
import com.kato.pro.common.utils.JsonUtils;
import com.kato.pro.rec.entity.constant.AbOrNacosConstant;
import com.kato.pro.rec.entity.core.RsInfo;
import com.kato.pro.rec.entity.enums.RsEnum;
import com.kato.pro.rec.service.core.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RetrieveCaptor {

    @Resource private UserService userService;

    /**
     * 获取召回源：
     * 1. 从abMap中获取；
     * 2. 从配置文件中获取全量的召回源，abMap优先级更高，如果有冲突的以abMap为主
     */
    public List<RsInfo> wrapRecallSources(Map<String, String> abMap) {
        String rsStr = abMap.get(AbOrNacosConstant.USER_RECALL_SOURCE);
        Set<RsInfo> rsSet = new HashSet<>();
        // 包装ab
        wrapAbRecallSources(rsStr, rsSet);
        // 包装全量的召回源
        wrapDefaultRecallSources(rsSet);
        // 初始化某些属性
        rsSet.forEach(RsInfo::init);
        // 过滤某些不符合条件的召回源，比如说某些召回源码只允许新人，某些只允许某个地区的人使用等等
        return filterRecallSources(rsSet, abMap);
    }

    // 从ab配置中包装召回源
    private void wrapAbRecallSources(String rsStr, Set<RsInfo> rsSet) {
        if (CharSequenceUtil.isBlank(rsStr) || BaseConstant.EMPTY_BRACKET.equals(rsStr)) {
            return;
        }
        rsSet.addAll(Objects.requireNonNull(JsonUtils.toList(rsStr, RsInfo.class)));
    }

    // 从配置中获取
    private void wrapDefaultRecallSources(Set<RsInfo> rsSet) {
        // normal    new,old,cold_start  .....
        String normalRsStr = ConfigUtils.getProperty(AbOrNacosConstant.DEFAULT_NORMAL_RECALL_SOURCES);
        if (CharSequenceUtil.isNotBlank(normalRsStr)) {
            Splitter.on(",")
                    .trimResults()
                    .splitToList(normalRsStr)
                    .stream()
                    .filter(CharSequenceUtil::isNotBlank)
                    .map(RsInfo::new)
                    .forEach(rsSet::add);
        }

        // special
        String specialRsStr = ConfigUtils.getProperty(AbOrNacosConstant.DEFAULT_SPECIAL_RECALL_SOURCES);
        if (CharSequenceUtil.isNotBlank(specialRsStr) && !BaseConstant.EMPTY_BRACKET.equals(specialRsStr)) {
            rsSet.addAll(Objects.requireNonNull(JsonUtils.toList(specialRsStr, RsInfo.class)));
        }
    }

    private List<RsInfo> filterRecallSources(Set<RsInfo> rsSet, Map<String, String> abMap) {
        // 赋值label，过滤不可用的rs
        filterBaseAndInitialize(rsSet);
        // 新老用户
        filterRecallSourcesAccessibleUser(rsSet, abMap);
        // 唯一召回源过滤
        rsSet = filterRecallSourcesByUnique(rsSet, abMap);
        return new ArrayList<>(rsSet);
    }

    /**
     * 填充
     */
    private void filterBaseAndInitialize(Set<RsInfo> rsSet) {
        Map<String, RsEnum> mapping = Arrays.stream(RsEnum.values())
                .filter(RsEnum::getEnable)
                .collect(Collectors.toMap(RsEnum::getCode, Function.identity()));
        Iterator<RsInfo> iterator = rsSet.iterator();
        while (iterator.hasNext()) {
            RsInfo next = iterator.next();
            RsEnum rsEnum = mapping.get(next.getRsName());
            if (rsEnum == null) {
                iterator.remove();
                continue;
            }
            next.setLabel(rsEnum.getLabel());
            next.init();
        }
    }

    /**
     * 根据新老用户过滤召回源
     */
    private void filterRecallSourcesAccessibleUser(Set<RsInfo> rsSet, Map<String, String> abMap) {
        // 不用过滤的直接返回
        if (CollUtil.isEmpty(rsSet)) return;
        RsInfo rsInfo = rsSet.stream().filter(item -> Objects.nonNull(item.getAccessible())).findFirst().orElse(null);
        if (rsInfo != null) return;

        Boolean isNewUser = userService.isNewUser(abMap);
        Iterator<RsInfo> iterator = rsSet.iterator();
        while (iterator.hasNext()) {
            RsInfo next = iterator.next();
            Boolean accessible = next.getAccessible();
            if (Objects.isNull(accessible)) {
                continue;
            }
            if (!isNewUser.equals(accessible)) {
                iterator.remove();
            }
        }
    }

    /**
     * 如果有唯一召回源，就返回label号比较大的那个
     */
    private Set<RsInfo> filterRecallSourcesByUnique(Set<RsInfo> rsSet, Map<String, String> abMap) {
        for (RsInfo rsInfo : rsSet) {
            if (Boolean.TRUE.equals(rsInfo.getUnique())) {
                return Sets.newHashSet(rsInfo);
            }
        }
        return rsSet;
    }


}
