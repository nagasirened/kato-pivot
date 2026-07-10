package com.kato.pro.langchain.domain.sync;

import com.kato.pro.langchain.domain.knowledge.SourceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ProductCatalogAdapter 单元测试。
 *
 * v1 adapter 返回 mock 5 条产品；测试聚焦于：
 *   - name() / sourceType() 标识正确
 *   - fetchSince 返回 5 条 record，字段齐全
 */
class ProductCatalogAdapterTest {

    private final ProductCatalogAdapter adapter = new ProductCatalogAdapter();

    @Test
    void name_returnsProductCatalog() {
        assertEquals("product_catalog", adapter.name());
        assertEquals(SyncSource.PRODUCT.adapterName(), adapter.name());
    }

    @Test
    void sourceType_returnsSyncProduct() {
        assertEquals(SourceType.SYNC_PRODUCT, adapter.sourceType());
    }

    @Test
    void fetchSince_returns5Records() {
        List<SyncRecord> records = adapter.fetchSince(null);
        assertNotNull(records);
        assertEquals(5, records.size());
        for (SyncRecord r : records) {
            assertNotNull(r.getExternalId());
            assertNotNull(r.getTitle());
            assertNotNull(r.getContent());
            assertTrue(r.getContent().length() > 0);
            assertNotNull(r.getTags());
            assertNotNull(r.getUpdatedAt());
        }
        assertTrue(records.stream().anyMatch(r -> "PROD-001".equals(r.getExternalId())));
    }

    @Test
    void fetchSince_withLastSyncTime_returnsAllInV1() {
        // v1 mock 不过滤 lastSyncTime，全量返回
        List<SyncRecord> records = adapter.fetchSince(Instant.now().minusSeconds(3600));
        assertEquals(5, records.size());
    }
}
