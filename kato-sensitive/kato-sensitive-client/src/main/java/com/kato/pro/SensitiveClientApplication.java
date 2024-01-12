package com.kato.pro;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
@RefreshScope
public class SensitiveClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(SensitiveClientApplication.class, args);
    }

    @Value("${kato.aws.enable}")
    public String name;

    @GetMapping("/info")
    public String returnStr() {
        return "str is " + name + " end ";
    }

}
