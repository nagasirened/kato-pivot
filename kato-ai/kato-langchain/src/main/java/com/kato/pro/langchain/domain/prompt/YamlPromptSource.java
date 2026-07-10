package com.kato.pro.langchain.domain.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * YAML 模板源（classpath:prompts/default.yml）。
 *
 * 文件格式：
 *   templates:
 *     rag_chat: |
 *       ...
 *     chitchat: |
 *       ...
 */
@Slf4j
public class YamlPromptSource {

    private static final String DEFAULT_PATH = "classpath:prompts/default.yml";

    private final List<PromptTemplateVo> templates = new ArrayList<>();

    public YamlPromptSource(ResourceLoader loader) {
        Resource res = loader.getResource(DEFAULT_PATH);
        if (!res.exists()) {
            log.warn("YAML prompt source not found: {}", DEFAULT_PATH);
            return;
        }
        try (InputStream in = res.getInputStream()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = new Yaml().load(in);
            if (root == null) return;
            Object ts = root.get("templates");
            if (!(ts instanceof Map)) return;
            @SuppressWarnings("unchecked")
            Map<String, Object> tMap = (Map<String, Object>) ts;
            for (Map.Entry<String, Object> e : tMap.entrySet()) {
                templates.add(new PromptTemplateVo(
                        null, e.getKey(), String.valueOf(e.getValue()), 1, "YAML"));
            }
            log.info("Loaded {} YAML prompt templates", templates.size());
        } catch (Exception ex) {
            log.error("Failed to load YAML prompts from {}", DEFAULT_PATH, ex);
        }
    }

    public List<PromptTemplateVo> loadAll() {
        return Collections.unmodifiableList(templates);
    }

    /** 仅测试用：直接塞模板 */
    void addAll(List<PromptTemplateVo> ts) {
        templates.addAll(ts);
    }
}
