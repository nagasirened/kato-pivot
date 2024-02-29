package com.kato.pro.rec.utilities;

import cn.hutool.core.collection.CollUtil;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.service.ItemFilterService;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Set;

@UtilityClass
public class RecommendUtils {

    public List<RecommendItem> ridItemsOfRes(List<RecommendItem> items, Set<Integer> trash) {
        if (CollUtil.isEmpty(items) || CollUtil.isEmpty(trash)) {
            return items;
        }
        items.removeIf(next -> trash.contains(next.getItemId()));
        return ItemFilterService.filterClosedV1(items);
    }


}
