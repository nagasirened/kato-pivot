package com.kato.pro.langchain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.kato.pro.langchain")
@EnableConfigurationProperties
public class LangChain4jApplication {

    public static void main(String[] args) {
        SpringApplication.run(LangChain4jApplication.class, args);
    }

}
