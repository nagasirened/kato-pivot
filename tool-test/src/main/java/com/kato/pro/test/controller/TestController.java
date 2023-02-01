package com.kato.pro.test.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api("测试接口")
@RestController
@RequestMapping("/test")
public class TestController {

    @ApiOperation(value = "swagger", notes = "用来测试swagger接口")
    @PostMapping("/swagger")
    public String testFor() {
        return "ok";
    }

}
