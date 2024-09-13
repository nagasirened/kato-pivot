package com.kato.pro.base.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties("kato.filters")
public class FilterProperties {

    private List<FilterConf> includes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(of = "path")
    public static class FilterConf {
        private String path;
        private String urlPatterns;
        private Integer order;
    }

}
