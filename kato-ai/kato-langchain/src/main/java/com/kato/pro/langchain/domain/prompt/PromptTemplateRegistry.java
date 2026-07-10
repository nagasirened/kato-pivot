package com.kato.pro.langchain.domain.prompt;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模板注册表（D3=C：DB > YAML > 内置）。
 *
 * 加载策略（D5=C）：
 *   - @PostConstruct：启动时从 YAML + DB 加载到内存
 *   - @Scheduled：每 5min 刷新一次（生产可调）
 *
 * 查询规则（get(key, tenantId)）：
 *   1. DB 行（tenantId 匹配）→ 用它
 *   2. YAML 行（key 匹配）  → 用它
 *   3. 都没有 → Optional.empty()
 */
@Slf4j
@RequiredArgsConstructor
public class PromptTemplateRegistry {

    private final YamlPromptSource yamlSource;
    private final DbPromptSource dbSource;

    /** 租户级：tenantId → (key → template) */
    private final Map<Long, Map<String, PromptTemplateVo>> byTenant = new ConcurrentHashMap<>();
    /** 系统级：key → template（来源 YAML 或 DB 系统级） */
    private final Map<String, PromptTemplateVo> systemLevel = new ConcurrentHashMap<>();

    private volatile boolean loaded = false;

    @PostConstruct
    public void init() {
        reload();
    }

    @Scheduled(fixedDelayString = "${prompt.refresh-interval-ms:300000}", initialDelay = 300_000)
    public void reload() {
        try {
            List<PromptTemplateVo> yaml = yamlSource.loadAll();
            List<PromptTemplateVo> db = dbSource.loadAll();
            rebuild(yaml, db);
            loaded = true;
            log.info("PromptTemplateRegistry reloaded: yaml={}, db={}", yaml.size(), db.size());
        } catch (Exception e) {
            log.error("PromptTemplateRegistry reload failed; keeping previous snapshot", e);
        }
    }

    private void rebuild(List<PromptTemplateVo> yaml, List<PromptTemplateVo> db) {
        Map<Long, Map<String, PromptTemplateVo>> nextTenant = new ConcurrentHashMap<>();
        Map<String, PromptTemplateVo> nextSystem = new ConcurrentHashMap<>();
        for (PromptTemplateVo t : yaml) {
            nextSystem.put(t.key(), t);
        }
        for (PromptTemplateVo t : db) {
            if (t.tenantId() == null) {
                nextSystem.put(t.key(), t);
            } else {
                nextTenant.computeIfAbsent(t.tenantId(), k -> new ConcurrentHashMap<>())
                        .put(t.key(), t);
            }
        }
        byTenant.clear();
        byTenant.putAll(nextTenant);
        systemLevel.clear();
        systemLevel.putAll(nextSystem);
    }

    public Optional<PromptTemplateVo> get(String key, Long tenantId) {
        if (key == null) return Optional.empty();
        if (tenantId != null) {
            Map<String, PromptTemplateVo> t = byTenant.get(tenantId);
            if (t != null) {
                PromptTemplateVo override = t.get(key);
                if (override != null) return Optional.of(override);
            }
        }
        return Optional.ofNullable(systemLevel.get(key));
    }

    public List<PromptTemplateVo> listAll(Long tenantId) {
        List<PromptTemplateVo> all = new ArrayList<>(systemLevel.values());
        if (tenantId != null) {
            Map<String, PromptTemplateVo> t = byTenant.get(tenantId);
            if (t != null) all.addAll(t.values());
        }
        return all;
    }

    public boolean isLoaded() { return loaded; }
}
