package com.kato.pro.langchain.domain.prompt;

import com.kato.pro.langchain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PromptTemplateService 纯方法/参数校验测试。
 *
 * 注：完整 CRUD（含 mapper 调用）的端到端测试受 sandbox 限制（不能用 Mockito，
 * MyBatis-Plus Mapper 接口 stub 实现过于繁冗），留待非沙箱环境的集成测试覆盖。
 */
class PromptTemplateServiceTest {

    @Test
    void create_nullKey_throws() {
        PromptTemplateService svc = new PromptTemplateService(null, null);
        assertThrows(BusinessException.class, () -> svc.create(1L, null, "x", null));
    }

    @Test
    void create_blankKey_throws() {
        PromptTemplateService svc = new PromptTemplateService(null, null);
        assertThrows(BusinessException.class, () -> svc.create(1L, "", "x", null));
    }

    @Test
    void create_nullContent_throws() {
        PromptTemplateService svc = new PromptTemplateService(null, null);
        assertThrows(BusinessException.class, () -> svc.create(1L, "k", null, null));
    }
}
