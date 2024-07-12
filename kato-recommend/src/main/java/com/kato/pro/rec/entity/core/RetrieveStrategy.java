package com.kato.pro.rec.entity.core;

import com.kato.pro.rec.entity.po.RecommendParams;

import java.util.List;

public interface RetrieveStrategy {
    List<RecommendItem> recall(RsInfo rsInfo, RecommendParams params);
}
