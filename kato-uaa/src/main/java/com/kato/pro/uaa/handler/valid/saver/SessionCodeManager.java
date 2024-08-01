package com.kato.pro.uaa.handler.valid.saver;

import com.kato.pro.uaa.entity.code.ValidateCode;
import org.springframework.social.connect.web.HttpSessionSessionStrategy;
import org.springframework.social.connect.web.SessionStrategy;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Optional;

public class SessionCodeManager implements ICodeManager {

    SessionStrategy sessionStrategy = new HttpSessionSessionStrategy();

    @Override
    public void save(ServletWebRequest webRequest, ValidateCode validateCode) {
        String key = wrapSaveKey(validateCode.getValidPrefix());
        sessionStrategy.setAttribute(webRequest, key, validateCode);
    }

    @Override
    public String getCode(String prefix, ServletWebRequest webRequest) {
        return Optional.ofNullable(sessionStrategy.getAttribute(webRequest, wrapSaveKey(prefix)))
                .map(String::valueOf)
                .orElse("");
    }

}
