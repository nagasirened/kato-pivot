package com.kato.pro.rec.entity.core;

import com.kato.pro.rec.entity.po.RecommendParams;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecallBox {

    private RsInfo rsInfo;
    private RecommendParams params;

}
