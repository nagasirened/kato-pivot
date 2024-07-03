package com.kato.pro.base.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties("kato.filters")
public class FilterProperties {

    private List<FilterConf> includes;

    @Getter
    @Setter
    public static class FilterConf {
        private String path;
        private String UrlPatterns;
        private Integer order;
    }

}
