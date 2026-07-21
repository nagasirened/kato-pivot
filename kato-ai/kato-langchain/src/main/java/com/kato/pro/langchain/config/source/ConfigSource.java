package com.kato.pro.langchain.config.source;

import java.util.function.Consumer;

/**
 * 配置源抽象 SPI。
 *
 * 责任：
 *   - 暴露 getSnapshot() 让业务方拉取最新配置
 *   - 暴露 onChange(callback) 让业务方订阅变更事件
 *
 * 实现方：
 *   - LocalConfigSource：从 Spring Environment 拉（默认实现，沙箱友好）
 *   - M17+ NacosConfigSource：从 Nacos server 拉（未来）
 *   - M17+ ApolloConfigSource：从 Apollo 拉（未来）
 *
 * 关键约定：
 *   - getSnapshot() 必须线程安全
 *   - onChange 回调在调用线程同步触发（实现方决定是否异步）
 *   - listener 必须能处理重复触发（同 key 同 value 也可能触发）
 */
public interface ConfigSource {

    /**
     * 来源标识（用于日志 / metric / 调试）。
     */
    String sourceName();

    /**
     * 拉取当前配置快照。永远不返回 null。
     */
    ConfigSnapshot getSnapshot();

    /**
     * 订阅变更。当配置发生变更时调用 callback。
     *
     * @return handle — 调用 close() 取消订阅
     */
    AutoCloseable onChange(Consumer<ConfigSnapshot> callback);

    /**
     * 默认空操作 handle。
     */
    AutoCloseable NO_OP_HANDLE = () -> { };
}
