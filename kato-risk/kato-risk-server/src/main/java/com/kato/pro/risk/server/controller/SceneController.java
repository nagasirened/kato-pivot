package com.kato.pro.risk.server.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 场景配置 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/risk/scenes")
public class SceneController {

    // 内存中的场景配置（实际应持久化到数据库或配置中心）
    private static final Map<String, SceneConfig> SCENE_CONFIGS = new HashMap<>();

    static {
        SCENE_CONFIGS.put("REGISTER", new SceneConfig("REGISTER", true, "PASS"));
        SCENE_CONFIGS.put("LOGIN", new SceneConfig("LOGIN", true, "PASS"));
        SCENE_CONFIGS.put("MARKETING", new SceneConfig("MARKETING", true, "PASS"));
        SCENE_CONFIGS.put("ORDER", new SceneConfig("ORDER", true, "PASS"));
        SCENE_CONFIGS.put("PAYMENT", new SceneConfig("PAYMENT", true, "PASS"));
        SCENE_CONFIGS.put("REFUND", new SceneConfig("REFUND", true, "PASS"));
    }

    /**
     * 获取所有场景配置
     */
    @GetMapping
    public Map<String, SceneConfig> getAllScenes() {
        return SCENE_CONFIGS;
    }

    /**
     * 启用/停用场景
     */
    @PutMapping("/{scene}/toggle")
    public SceneConfig toggleScene(@PathVariable String scene,
                                    @RequestParam(required = false) Boolean enabled,
                                    @RequestParam(required = false) String defaultAction) {
        SceneConfig config = SCENE_CONFIGS.get(scene.toUpperCase());
        if (config == null) {
            throw new RuntimeException("场景不存在: " + scene);
        }

        if (enabled != null) {
            config.setEnabled(enabled);
        }
        if (defaultAction != null) {
            config.setDefaultAction(defaultAction);
        }

        log.info("[SceneController] scene={} enabled={} defaultAction={}",
                scene, config.isEnabled(), config.getDefaultAction());
        return config;
    }

    @Data
    public static class SceneConfig {
        private String scene;
        private boolean enabled;
        private String defaultAction;

        public SceneConfig() {}

        public SceneConfig(String scene, boolean enabled, String defaultAction) {
            this.scene = scene;
            this.enabled = enabled;
            this.defaultAction = defaultAction;
        }
    }
}