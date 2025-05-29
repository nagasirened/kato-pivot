package com.kato.pro.test.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.vavr.Tuple;
import io.vavr.Tuple5;
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

    public static void main(String[] args) throws Throwable {
        Tuple5<Integer, String, Double, Boolean, Long> tupleArgs = Tuple.of(1, "test", 2.5, true, 100L);
        Integer i = tupleArgs._1;
        String s = tupleArgs._2;

    }

}
