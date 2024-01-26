package com.kato.pro.rec.utilities;

import cn.hutool.core.collection.CollUtil;
import com.kato.pro.rec.entity.core.RecommendItem;
import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.Set;

@UtilityClass
public class RecommendUtils {

    public void ridItemsOfRes(Collection<RecommendItem> items, Set<Integer> trash) {
        if (CollUtil.isEmpty(items) || CollUtil.isEmpty(trash)) {
            return;
        }
        items.removeIf(next -> trash.contains(next.getItemId()));
    }


}
