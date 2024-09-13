package com.kato.pro.uaa.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "kato.security")
public final class SecurityProperties {

    private BrowserProperties browser = new BrowserProperties();

    private ValidateCodeProperties valid = new ValidateCodeProperties();

}