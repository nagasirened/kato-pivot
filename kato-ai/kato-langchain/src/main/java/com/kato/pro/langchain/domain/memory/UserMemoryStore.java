package com.kato.pro.langchain.domain.memory;

import java.util.List;
import java.util.Optional;

/**
 * 长期记忆存储（U1 — v1 仅预留接口，不实装）。
 *
 * 设计目标：跨 session 持久化"用户偏好 / 关键事实"，供主对话引擎注入 prompt。
 * v1：NoOp 默认实现；v2：可换 MySQL / Redis 实现。
 */
public interface UserMemoryStore {

    Optional<String> recall(Long userId, String key);

    void remember(Long userId, String key, String value);

    List<String> listKeys(Long userId);
}
