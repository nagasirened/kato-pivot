package com.kato.pro.base.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.kato.pro.base.resolver.KatoHeaderFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;

import javax.annotation.Resource;
import javax.servlet.Filter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EnableConfigurationProperties({FilterProperties.class})
public class FilterChainConfig {
    private static final Logger log = LoggerFactory.getLogger(FilterChainConfig.class);
    @Resource private FilterProperties filterProperties;

    /**
     * 将配置中的filter加入的调用链路中
     */
    @Bean
    public List<FilterRegistrationBean<?>> registrationBean() {
        List<FilterRegistrationBean<?>> beans = new ArrayList<>();
        List<FilterProperties.FilterConf> includes = Stream.concat(filterProperties.getIncludes().stream(), defaultFilter().stream()).distinct().collect(Collectors.toList());
        for (FilterProperties.FilterConf temp : includes) {
            String path = temp.getPath();
            if (StrUtil.isBlank(path)) {
                continue;
            }
            String urlPatternsStr = temp.getUrlPatterns();
            List<String> patterns = StrUtil.isBlank(urlPatternsStr) ?
                    ImmutableList.of("/*") : Splitter.on(",").trimResults().splitToList(urlPatternsStr);

            try {
                Class<?> clazz = Class.forName(path);
                FilterRegistrationBean<Filter> loginUserFilterBean = new FilterRegistrationBean<>();
                Filter filter = (Filter) clazz.getDeclaredConstructor().newInstance();
                loginUserFilterBean.setFilter(filter);
                loginUserFilterBean.addUrlPatterns(patterns.toArray(new String[0]));
                loginUserFilterBean.setOrder(Optional.ofNullable(temp.getOrder()).orElse(100));
                beans.add(loginUserFilterBean);
            } catch (ClassNotFoundException e) {
                log.error("Incorrect package path, classPath is {}", path);
            } catch (NoSuchMethodException e) {
                log.error("There is no default parameterless constructor, classPath is {}", path);
            } catch (Exception e) {
                log.error("Failed to initialize the interceptor, classPath is {}", path);
            }
        }
        return beans;
    }

    public List<FilterProperties.FilterConf> defaultFilter() {
        return ImmutableList.of(
                new FilterProperties.FilterConf(com.kato.pro.base.resolver.KatoHeaderFilter.class.getTypeName(), "/*", 100),
                new FilterProperties.FilterConf(com.kato.pro.base.resolver.LoginUserFilter.class.getTypeName(), "/*", 100),
                new FilterProperties.FilterConf(com.kato.pro.base.resolver.TraceFilter.class.getTypeName(), "/*", 100)
        );
    }

}
