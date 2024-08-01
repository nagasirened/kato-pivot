package com.kato.pro.uaa.handler.valid.processor;

import org.springframework.web.context.request.ServletWebRequest;

import java.io.IOException;

public interface ValidateCodeProcessor {

    void createValidateCode(ServletWebRequest servletWebRequest) throws IOException;

}
