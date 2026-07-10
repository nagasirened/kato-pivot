package com.kato.pro.langchain.domain.prompt;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptTemplateRegistryTest {

    private PromptTemplateVo yaml(String key, String content) {
        return new PromptTemplateVo(null, key, content, 1, "YAML");
    }

    private PromptTemplateVo db(Long tenantId, String key, String content) {
        return new PromptTemplateVo(tenantId, key, content, 1, "DB");
    }

    @Test
    void get_prefersDbTenantOverride() {
        YamlPromptSource yaml = stubYaml(yaml("rag", "YAML-RAG"));
        DbPromptSource db = stubDb(db(1L, "rag", "DB-OVERRIDE-RAG"));
        PromptTemplateRegistry r = new PromptTemplateRegistry(yaml, db);
        r.reload();

        assertEquals("DB-OVERRIDE-RAG", r.get("rag", 1L).orElseThrow().content());
    }

    @Test
    void get_fallsBackToYaml_whenNoDb() {
        YamlPromptSource yaml = stubYaml(yaml("chitchat", "YAML-CHITCHAT"));
        DbPromptSource db = stubDb();
        PromptTemplateRegistry r = new PromptTemplateRegistry(yaml, db);
        r.reload();

        assertEquals("YAML-CHITCHAT", r.get("chitchat", 1L).orElseThrow().content());
    }

    @Test
    void get_otherTenant_fallsBackToSystemLevel() {
        YamlPromptSource yaml = stubYaml(yaml("rag", "YAML-RAG"));
        DbPromptSource db = stubDb(db(1L, "rag", "DB-T1-RAG"));
        PromptTemplateRegistry r = new PromptTemplateRegistry(yaml, db);
        r.reload();

        assertEquals("YAML-RAG", r.get("rag", 2L).orElseThrow().content());
    }

    @Test
    void get_missing_returnsEmpty() {
        PromptTemplateRegistry r = new PromptTemplateRegistry(stubYaml(), stubDb());
        r.reload();
        assertTrue(r.get("nope", 1L).isEmpty());
    }

    @Test
    void reload_failure_keepsPreviousSnapshot() {
        // 测试目标：reload() 内部捕获异常，保留之前的快照。
        // 策略：第一次 reload 成功（用 db），第二次替换成 throwing db，reload 抛错但快照保留。
        YamlPromptSource yaml = stubYaml();
        DbPromptSource db = stubDb(db(1L, "a", "first"));
        PromptTemplateRegistry r = new PromptTemplateRegistry(yaml, db);
        r.reload();
        assertEquals("first", r.get("a", 1L).orElseThrow().content());

        // 用反射替换 dbSource 为 throwing
        DbPromptSource throwing = () -> { throw new RuntimeException("boom"); };
        try {
            Field f = PromptTemplateRegistry.class.getDeclaredField("dbSource");
            f.setAccessible(true);
            f.set(r, throwing);
        } catch (Exception e) { throw new RuntimeException(e); }
        r.reload();
        // 快照保留 → "first" 仍然可取
        assertEquals("first", r.get("a", 1L).orElseThrow().content());
    }

    @Test
    void listAll_includesSystemAndTenant() {
        YamlPromptSource yaml = stubYaml(yaml("a", "sys-a"), yaml("b", "sys-b"));
        DbPromptSource db = stubDb(db(1L, "a", "t1-a"));
        PromptTemplateRegistry r = new PromptTemplateRegistry(yaml, db);
        r.reload();

        List<PromptTemplateVo> all = r.listAll(1L);
        assertEquals(3, all.size());
    }

    // ---- stubs ----
    private YamlPromptSource stubYaml(PromptTemplateVo... ts) {
        YamlPromptSource y = new YamlPromptSource(new DefaultResourceLoader());
        // 清空 classpath:prompts/default.yml 的真实加载项，避免污染 stub 测试
        try {
            Field f = YamlPromptSource.class.getDeclaredField("templates");
            f.setAccessible(true);
            ((java.util.List<?>) f.get(y)).clear();
        } catch (Exception e) { throw new RuntimeException(e); }
        y.addAll(Arrays.asList(ts));
        return y;
    }

    private DbPromptSource stubDb(PromptTemplateVo... ts) {
        return () -> Arrays.asList(ts);
    }
}
