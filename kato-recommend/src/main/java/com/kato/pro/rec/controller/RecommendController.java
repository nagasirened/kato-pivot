package com.kato.pro.rec.controller;

import com.kato.pro.common.constant.BaseConstant;
import com.kato.pro.base.entity.Result;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.entity.po.RecommendRequest;
import com.kato.pro.rec.service.RecommendService;
import com.kato.pro.rec.service.core.RateGateway;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/recommend/v1")
public class RecommendController {

    @Resource private RecommendService recommendService;
    @Resource private RateGateway rateGateway;

    @PostMapping
    public Result<List<RecommendItem>> recommend(@RequestBody RecommendRequest request) {
        return Result.build(recommendService.recommend(request));
    }

    @GetMapping
    public Result<String> refreshRate() {
        rateGateway.init();
        return Result.build(BaseConstant.SUCCESS);
    }

}
