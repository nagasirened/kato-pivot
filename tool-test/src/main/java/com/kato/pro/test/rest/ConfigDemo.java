package com.kato.pro.test.rest;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;

import java.util.function.Predicate;

@Configurable
public class ConfigDemo {

    @Bean(name = "myFailurePredicate")
    public Predicate<Throwable> myFailurePredicate() {
        return throwable -> throwable instanceof RuntimeException;
    }

}
