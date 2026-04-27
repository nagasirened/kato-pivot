package com.kato.pro.sensitive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.kato.pro"}, exclude = {AopAutoConfiguration.class})
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
public class SensitiveServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SensitiveServerApplication.class, args);
    }
}
