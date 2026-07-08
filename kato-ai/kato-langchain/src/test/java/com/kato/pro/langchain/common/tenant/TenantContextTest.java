package com.kato.pro.langchain.common.tenant;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        // 防止测试间 ThreadLocal 泄漏
        TenantContext.clear();
    }

    @Test
    void requireCurrent_whenNotSet_throwsBusinessException() {
        TenantContext.clear();
        BusinessException ex = assertThrows(BusinessException.class, TenantContext::requireCurrent);
        assertEquals(ErrorCode.TENANT_MISMATCH, ex.getErrorCode());
    }

    @Test
    void requireCurrent_whenSet_returnsTenantInfo() {
        TenantContext.set(TenantInfo.of(100L, 1L));
        TenantInfo info = TenantContext.requireCurrent();
        assertEquals(100L, info.tenantId());
        assertEquals(1L, info.userId());
    }

    @Test
    void currentOrNull_whenNotSet_returnsNull() {
        TenantContext.clear();
        assertNull(TenantContext.currentOrNull());
    }

    @Test
    void clear_removesValue() {
        TenantContext.set(TenantInfo.of(200L, 2L));
        TenantContext.clear();
        assertNull(TenantContext.currentOrNull());
    }

    @Test
    void currentTenantId_andUserId_delegateToInfo() {
        TenantContext.set(TenantInfo.of(300L, 30L));
        assertEquals(300L, TenantContext.currentTenantId());
        assertEquals(30L, TenantContext.currentUserId());
    }

    @Test
    void tenantInfo_rejectsNullTenantId() {
        assertThrows(IllegalArgumentException.class, () -> new TenantInfo(null, 1L, "DEFAULT"));
    }

    @Test
    void tenantInfo_rejectsNullUserId() {
        assertThrows(IllegalArgumentException.class, () -> new TenantInfo(1L, null, "DEFAULT"));
    }
}
