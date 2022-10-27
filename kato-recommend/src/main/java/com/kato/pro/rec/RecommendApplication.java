package com.kato.pro.rec;


import com.kato.pro.redisson.lock.annotation.EnableRedissonLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableRedissonLock
@EnableDiscoveryClient
public class RecommendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendApplication.class, args);
    }

}
