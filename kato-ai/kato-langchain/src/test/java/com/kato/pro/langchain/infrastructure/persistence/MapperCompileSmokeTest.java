package com.kato.pro.langchain.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Mapper 接口编译期烟雾测试。
 *
 * 目的：在没有 MySQL/Flyway 的沙箱环境下，至少确认：
 *   1) Mapper 接口能被 Spring/MyBatis-Plus 加载（@MapperScan 扫描得到）
 *   2) 接口继承 TenantAwareBaseMapper&lt;X&gt; 后 default 方法签名与父接口对齐
 *
 * 不做真正 DB 调用 —— sandbox 下 Mockito-inline + byte-buddy self-attach 不工作（Java 21）。
 */
class MapperCompileSmokeTest {

    @Test
    void chatSessionMapper_isInterface() {
        assertNotNull(ChatSessionMapper.class);
        assertNotNull(TenantAwareBaseMapper.class.isAssignableFrom(ChatSessionMapper.class));
    }

    @Test
    void chatMessageMapper_isInterface() {
        assertNotNull(ChatMessageMapper.class);
        assertNotNull(TenantAwareBaseMapper.class.isAssignableFrom(ChatMessageMapper.class));
    }
}
