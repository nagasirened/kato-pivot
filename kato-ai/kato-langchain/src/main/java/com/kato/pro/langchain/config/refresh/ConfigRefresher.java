package com.kato.pro.langchain.config.refresh;

import com.kato.pro.langchain.config.source.ConfigSnapshot;
import com.kato.pro.langchain.config.source.ConfigSource;
import com.kato.pro.langchain.config.source.LocalConfigSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 配置刷新协调器。
 *
 * 责任：
 *   - 接受业务方 register(callback) 订阅
 *   - 提供 refreshNow() 主动刷新入口
 *   - 监听 KatoConfigChangeEvent，把事件翻译成本地 listener 调用
 *   - 自身在 refreshNow() 时发布 KatoConfigChangeEvent（让其他 Bean 也能响应）
 *
 * 业务方接入：
 *   - 注入 ConfigRefresher → register(callback) 主动订阅
 *   - 或在自己 Bean 上加 @EventListener(KatoConfigChangeEvent.class) 监听事件
 *
 * 关键设计：
 *   - 不动现有 @ConfigurationProperties Bean（避免 M13 历史教训的 setter 删除风险）
 *   - listener 主动决定怎么 reload（re-pull Environment / 重新计算 cache / 触发指标更新）
 *   - listener 必须线程安全 + 异常隔离（一个挂了不影响其他人）
 *   - 不依赖 spring-cloud-context（沙箱 m2 不可用；M17+ 接 Nacos 后再考虑引入 @RefreshScope）
 */
@Slf4j
@Component
public class ConfigRefresher {

    private final ConfigSource configSource;
    private final ApplicationEventPublisher eventPublisher;
    private final CopyOnWriteArrayList<Consumer<ConfigSnapshot>> listeners = new CopyOnWriteArrayList<>();

    public ConfigRefresher(ConfigSource configSource) {
        this(configSource, null);
    }

    public ConfigRefresher(ConfigSource configSource, ApplicationEventPublisher eventPublisher) {
        this.configSource = configSource;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 注册监听器，立即触发一次。
     */
    public Handle register(Consumer<ConfigSnapshot> listener) {
        if (listener == null) return Handle.NO_OP;
        listeners.add(listener);
        try {
            listener.accept(configSource.getSnapshot());
        } catch (Exception e) {
            log.warn("Initial listener trigger failed: {}", e.getMessage());
        }
        return new Handle(() -> listeners.remove(listener));
    }

    /**
     * 监听 KatoConfigChangeEvent（M17+ NacosConfigSource 推送时发布）。
     * 重新拉快照并分发给本地 listener。
     */
    @EventListener(KatoConfigChangeEvent.class)
    public void onConfigChange(KatoConfigChangeEvent event) {
        log.info("KatoConfigChangeEvent received: reason={} version={} source={}",
                event.getReason(),
                event.getSnapshot() == null ? -1 : event.getSnapshot().version(),
                event.getSnapshot() == null ? "?" : event.getSnapshot().source());
        notifyAll(event.getSnapshot());
    }

    /**
     * 主动触发一次刷新（不依赖 Spring 事件）。
     *
     *   - 调 LocalConfigSource.refresh() 重新拉快照
     *   - 通知所有本地 listener
     *   - 发布 KatoConfigChangeEvent（让其他 Bean 也能响应）
     */
    public ConfigSnapshot refreshNow() {
        ConfigSnapshot fresh = configSource.getSnapshot();
        if (configSource instanceof LocalConfigSource local) {
            fresh = local.refresh();
        } else {
            // 其他 ConfigSource 实现不暴露强制 refresh — 用现有快照
            log.debug("ConfigSource {} does not support forced refresh; using cached snapshot",
                    configSource.sourceName());
        }
        notifyAll(fresh);
        publishEvent(fresh, "manual-refresh");
        return fresh;
    }

    private void publishEvent(ConfigSnapshot snapshot, String reason) {
        if (eventPublisher == null) return;
        try {
            eventPublisher.publishEvent(new KatoConfigChangeEvent(this, snapshot, reason));
        } catch (Exception e) {
            log.warn("Publish KatoConfigChangeEvent failed: {}", e.getMessage());
        }
    }

    private void notifyAll(ConfigSnapshot snapshot) {
        log.info("Config refresh broadcast: version={} keys={} source={}",
                snapshot.version(), snapshot.values().size(), snapshot.source());
        for (Consumer<ConfigSnapshot> l : listeners) {
            try {
                l.accept(snapshot);
            } catch (Exception e) {
                log.warn("Refresh listener failed: {}", e.getMessage(), e);
            }
        }
    }

    public int listenerCount() {
        return listeners.size();
    }

    /**
     * 句柄封装 — 调用 close() 取消注册。
     */
    public static final class Handle implements AutoCloseable {
        public static final Handle NO_OP = new Handle(() -> { });
        private final Runnable onClose;
        private volatile boolean closed = false;

        public Handle(Runnable onClose) {
            this.onClose = onClose;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            onClose.run();
        }
    }
}
