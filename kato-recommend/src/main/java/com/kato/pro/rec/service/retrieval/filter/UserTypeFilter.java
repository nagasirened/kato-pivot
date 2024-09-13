package com.kato.pro.rec.service.retrieval.filter;

import cn.hutool.core.collection.CollUtil;
import com.kato.pro.base.util.ConfigUtils;
import com.kato.pro.rec.entity.core.RsInfo;
import com.kato.pro.rec.service.core.UserService;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class UserTypeFilter implements IRetrievalFilter{

    @Override
    public int index() {
        return 2;
    }

    /**
     * 按照新老用户过滤
     */
    @Override
    public void consume(Set<RsInfo> rsSet, Map<String, String> abMap) {
        if (CollUtil.isEmpty(rsSet)) return;
        // 如果所有的召回源都没有配置新老用户过滤，直接返回
        RsInfo rsInfo = rsSet.stream().filter(item -> Objects.nonNull(item.getAccessible())).findFirst().orElse(null);
        if (rsInfo != null) return;

        UserService userService = ConfigUtils.getBean("userService", UserService.class);
        boolean isNewUser = userService.isNewUser(abMap);
        Iterator<RsInfo> iterator = rsSet.iterator();
        while (iterator.hasNext()) {
            RsInfo next = iterator.next();
            Boolean accessible = next.getAccessible();
            if (Objects.nonNull(accessible) && !accessible.equals(isNewUser)) {
                iterator.remove();
            }
        }
    }
}
