package com.kato.pro.langchain.domain.memory;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 默认空实现。所有操作返回空 / 静默成功。
 */
@Component
public class NoOpUserMemoryStore implements UserMemoryStore {

    @Override
    public Optional<String> recall(Long userId, String key) {
        return Optional.empty();
    }

    @Override
    public void remember(Long userId, String key, String value) {
        // no-op
    }

    @Override
    public List<String> listKeys(Long userId) {
        return List.of();
    }
}
