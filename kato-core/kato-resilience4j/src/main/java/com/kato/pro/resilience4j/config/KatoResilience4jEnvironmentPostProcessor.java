package com.kato.pro.resilience4j.config;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * 在环境准备阶段加载 jar 内 {@code kato-resilience4j-defaults.yml}，优先级最低，
 * 便于业务应用的 {@code application.yml} 覆盖同名 Resilience4j 配置项。
 */
public class KatoResilience4jEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private final YamlPropertySourceLoader yamlLoader = new YamlPropertySourceLoader();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Resource resource = new ClassPathResource("kato-resilience4j-defaults.yml");
        if (!resource.exists()) {
            return;
        }
        try {
            List<org.springframework.core.env.PropertySource<?>> sources =
                    yamlLoader.load("kato-resilience4j-defaults", resource);
            for (org.springframework.core.env.PropertySource<?> source : sources) {
                environment.getPropertySources().addLast(source);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load kato-resilience4j-defaults.yml", e);
        }
    }
}
