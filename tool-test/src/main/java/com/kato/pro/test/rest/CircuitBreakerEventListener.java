package com.kato.pro.test.rest;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.vavr.control.Try;
import org.springframework.stereotype.Component;


@Component
public class CircuitBreakerEventListener {

    public CircuitBreakerEventListener(CircuitBreakerRegistry circuitBreakerRegistry) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(DemoController.CIRCUIT_BREAKER);
        circuitBreaker.getEventPublisher().onStateTransition(this::handleStateTransition);
    }

    private void handleStateTransition(CircuitBreakerOnStateTransitionEvent circuitBreakerOnStateTransitionEvent) {
        System.out.println("断路器状态变化：" + circuitBreakerOnStateTransitionEvent.getStateTransition());
    }

    public static void main(String[] args) {
        // 封装可能抛出异常的操作
        Try<Integer> result = Try.of(() -> {
            // 模拟可能抛出异常的操作
            double random = Math.random();
            System.out.println(random);
            if (random < 0.5) {
                throw new RuntimeException("Something went wrong");
            }
            return 42;
        });

        // 处理成功和失败的结果
        result
                .onSuccess(value -> System.out.println("Operation succeeded with value: " + value))
                .onFailure(ex -> System.out.println("Operation failed with exception: " + ex.getMessage()))
                .recover(ex -> -1);

        // 提供默认值或备用操作
        Integer finalResult = result.getOrElse(0);
        System.out.println("Final result: " + finalResult);
        // 使用 recover 方法
        Try<Integer> recover = result.recover(ex -> {
            System.out.println("Recovering from exception: " + ex.getMessage());
            return -1;
        });
        System.out.println("Recovered result: " + recover.get());
    }

}
