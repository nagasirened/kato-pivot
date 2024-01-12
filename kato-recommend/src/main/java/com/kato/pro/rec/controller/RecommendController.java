package com.kato.pro.rec.controller;

import com.kato.pro.base.entity.Result;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.entity.po.RecommendRequest;
import com.kato.pro.rec.service.RecommendService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/recommend/v1")
public class RecommendController {

    @Resource private RecommendService recommendService;

    @PostMapping
    public Result<List<RecommendItem>> recommendV1(@RequestBody RecommendRequest request) {
        return Result.build(recommendService.recommend(request));
    }


}
