package com.kato.pro.uaa.controller;

import com.kato.pro.uaa.handler.valid.processor.ValidateCodeProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
public class ValidateCodeController {

    @Resource
    private Map<String, ValidateCodeProcessor> processorMap;

    @GetMapping("/validate/{type}")
    public void getImageCode(HttpServletRequest request, HttpServletResponse response, @PathVariable String type) throws Exception {
        processorMap.get(type + "CodeProcessor").createValidateCode(new ServletWebRequest(request, response));
    }

}
