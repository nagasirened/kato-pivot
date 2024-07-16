package com.kato.pro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
@RefreshScope
public class SensitiveClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(SensitiveClientApplication.class, args);
    }

}
