package com.kato.pro.langchain.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;
import com.kato.pro.langchain.common.trace.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * logback 配置加载测试。
 *
 * 沙箱踩坑：
 *   1. logback-spring.xml 仅在 Spring Boot 启动时由 ApplicationContext 加载
 *   2. 纯 JUnit 拿到的是 BasicConfigurator 的默认状态
 *   3. JoranConfigurator 加载 logback-spring.xml 时不识别 <springProfile> 标签，
 *      所有 profile 块被整体跳过
 *
 * 这里只验证：
 *   1. logback-spring.xml 在 classpath 上能找到（语法资源存在）
 *   2. 显式 JoranConfigurator 加载不抛异常（XML 语法 OK）
 *   3. traceId MDC roundTrip
 *
 * 实际生产配置验证靠 @SpringBootTest 启动日志 — 沙箱无此能力。
 */
class LogbackConfigTest {

    @AfterEach
    void tearDown() { TraceContext.clear(); }

    @Test
    void logbackSpringXml_isOnClasspath() {
        URL url = getClass().getClassLoader().getResource("logback-spring.xml");
        assertNotNull(url, "logback-spring.xml should be on classpath");
        assertTrue(url.getFile().endsWith("logback-spring.xml"));
    }

    @Test
    void logbackSpringXml_parsesWithoutError() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(ctx);
        try {
            URL url = getClass().getClassLoader().getResource("logback-spring.xml");
            configurator.doConfigure(url);
        } catch (Exception e) {
            StatusPrinter.printInCaseOfErrorsOrWarnings(ctx);
            throw new RuntimeException("logback-spring.xml parse failed: " + e.getMessage(), e);
        }
        // JoranConfigurator 不解析 <springProfile>，所以 root 没有 appender；
        // 这里只验证解析过程不抛异常即可。
    }

    @Test
    void traceIdMdc_roundTrip() {
        TraceContext.set("test-trace-id-001");
        assertEquals("test-trace-id-001", TraceContext.current());
        org.slf4j.Logger log = LoggerFactory.getLogger(LogbackConfigTest.class);
        log.debug("logback config roundtrip");
    }
}
