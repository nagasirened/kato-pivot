package com.kato.pro.langchain.config.source;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Nacos 真实连接冒烟测试。
 *
 * 启用条件：环境变量 NACOS_INTEGRATION=1
 *   - 默认 sandbox 不设 → 自动 skip（@EnabledIfEnvironmentVariable）
 *   - 部署环境设置后：跑真实 Nacos server 集成测试
 *
 * 跑法：
 *   mvn -pl kato-langchain test -Dtest=NacosIntegrationSmokeTest \
 *       -DNACOS_INTEGRATION=1 -DNACOS_SERVER=127.0.0.1:8848 \
 *       -DNACOS_DATA_ID=kato-langchain-test -DNACOS_GROUP=DEFAULT_GROUP
 */
class NacosIntegrationSmokeTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "NACOS_INTEGRATION", matches = "1")
    void realNacosServer_roundTrip() throws Exception {
        String server = System.getenv().getOrDefault("NACOS_SERVER", "127.0.0.1:8848");
        String dataId = System.getenv().getOrDefault("NACOS_DATA_ID", "kato-langchain-test");
        String group = System.getenv().getOrDefault("NACOS_GROUP", "DEFAULT_GROUP");

        NacosConfigProperties props = new NacosConfigProperties();
        props.setEnabled(true);
        props.setServerAddr(server);
        props.setGroup(group);
        props.setDataIds(java.util.List.of(dataId));
        props.setTimeoutMs(5000L);

        NacosConfigSource source = new NacosConfigSource(props);
        try {
            // 启动期已经拉过初始配置
            ConfigSnapshot initial = source.getSnapshot();
            assertNotNull(initial);
            assertEquals("nacos", initial.source());

            // 订阅推送
            AtomicReference<ConfigSnapshot> pushed = new AtomicReference<>();
            AutoCloseable handle = source.onChange(pushed::set);
            try {
                assertNotNull(pushed.get(), "onChange should fire immediately with current snapshot");
                assertTrue(pushed.get().version() > 0);
            } finally {
                handle.close();
            }
        } finally {
            source.close();
        }
    }
}
