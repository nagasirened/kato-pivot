package com.kato.pro.langchain.config.refresh;

import com.kato.pro.langchain.config.AuditJsonProperties;
import com.kato.pro.langchain.config.source.ConfigSnapshot;
import com.kato.pro.langchain.config.source.LocalConfigSource;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证 M16 设计的核心闭环：环境变更 → listener 拿到新快照 → 业务方能据此 reload。
 *
 * 这是 @RefreshScope 的轻量替代：
 *   - 不动现有 AuditJsonProperties（保留 @Data + @EnableConfigurationProperties 模式）
 *   - 业务方在 listener 里主动决定 reload（re-pull environment / 重算 cache / 通知指标）
 *
 * 场景模拟：运营改 application.yml 里的 kato.audit.file.path → listener 拿到新值。
 */
class RefreshScopeBehaviorTest {

    @Test
    void listenerSeesUpdatedPropertyAfterRefresh() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("kato.audit.file.path", "/var/log/audit-original.json");
        env.setProperty("kato.audit.file.enabled", "true");

        LocalConfigSource source = new LocalConfigSource();
        source.setEnvironment(env);

        // 业务方有 AuditJsonProperties（在生产中通过 @EnableConfigurationProperties 注入；
        // 这里直接 new 模拟 reload 行为）
        AuditJsonProperties props = new AuditJsonProperties();
        props.setPath(env.getProperty("kato.audit.file.path"));
        props.setEnabled(Boolean.parseBoolean(env.getProperty("kato.audit.file.enabled")));
        assertEquals("/var/log/audit-original.json", props.getPath());

        // 监听器：每次刷新时把新值写回 props
        ConfigRefresher refresher = new ConfigRefresher(source);
        ConfigRefresher.Handle h = refresher.register(snap -> {
            // 业务方 reload 逻辑：把 environment 变更反映到 props
            if (snap.has("kato.audit.file.path")) {
                props.setPath(snap.get("kato.audit.file.path"));
            }
            if (snap.has("kato.audit.file.enabled")) {
                props.setEnabled(Boolean.parseBoolean(snap.get("kato.audit.file.enabled")));
            }
        });

        // 模拟运营改配置（实际生产中是 Nacos 推送 / ConfigMap reload）
        env.setProperty("kato.audit.file.path", "/var/log/audit-rotated.json");
        env.setProperty("kato.audit.file.enabled", "false");
        refresher.refreshNow();

        // 验证 props 反映了新值
        assertEquals("/var/log/audit-rotated.json", props.getPath());
        assertEquals(false, props.isEnabled());

        h.close();
    }

    @Test
    void snapshotVersionIsMonotonic() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("kato.x", "1");
        LocalConfigSource source = new LocalConfigSource();
        source.setEnvironment(env);
        ConfigRefresher refresher = new ConfigRefresher(source);

        long[] versions = new long[5];
        ConfigRefresher.Handle h = refresher.register(snap -> {
            for (int i = 0; i < versions.length; i++) {
                if (versions[i] == 0) {
                    versions[i] = snap.version();
                    break;
                }
            }
        });

        for (int i = 0; i < 5; i++) {
            env.setProperty("kato.x", "v" + i);
            refresher.refreshNow();
        }

        // 版本号应单调递增
        for (int i = 1; i < versions.length; i++) {
            assertNotNull(versions[i]);
            assertEquals(true, versions[i] > versions[i - 1], "version should be monotonic");
        }
        h.close();
    }
}
