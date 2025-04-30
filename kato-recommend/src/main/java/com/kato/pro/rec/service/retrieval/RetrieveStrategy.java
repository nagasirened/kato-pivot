package com.kato.pro.rec.service.retrieval;

import com.kato.pro.rec.entity.core.RecallBox;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.entity.enums.RsEnum;

import java.util.List;

public interface RetrieveStrategy {

    RsEnum getRsEnum();

    List<RecommendItem> recall(RecallBox recallBox);

}
