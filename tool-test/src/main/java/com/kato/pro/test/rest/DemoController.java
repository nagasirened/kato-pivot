package com.kato.pro.test.rest;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    public static final String CIRCUIT_BREAKER = "demoCircuitBreaker";

    @GetMapping("/callService")
    @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "fallback")
    public String callService() {
        if (Math.random() < 0.5) {
            throw new RuntimeException("fail");
        }
        return "success";
    }

    public String fallback(Throwable t) {
        return "fallbackSuccess";
    }

}
