package com.kato.pro.langchain.domain.sync;

import com.kato.pro.langchain.domain.knowledge.SourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * v1 唯一示例适配器（spec §6 D3 + §8 YAGNI）。
 *
 * 数据源：mock 5 条产品（不连真实商品库；后续 v2 接真实业务库）。
 * 适配器模式：fetchSince 返回 List<SyncRecord>，SyncApplicationService 统一 upsert。
 */
@Slf4j
@Component
public class ProductCatalogAdapter implements SyncAdapter {

    @Override
    public String name() {
        return SyncSource.PRODUCT.adapterName();
    }

    @Override
    public SourceType sourceType() {
        return SyncSource.PRODUCT.sourceType();
    }

    @Override
    public List<SyncRecord> fetchSince(Instant lastSyncTime) {
        log.info("ProductCatalogAdapter.fetchSince lastSyncTime={}", lastSyncTime);
        List<SyncRecord> records = new ArrayList<>();
        Instant now = Instant.now();
        records.add(SyncRecord.builder()
                .externalId("PROD-001")
                .title("智能手表 X1")
                .content("X1 智能手表：AMOLED 屏幕、心率监测、GPS 定位、续航 14 天。")
                .tags(Map.of("category", "electronics", "price", "1299"))
                .updatedAt(now)
                .build());
        records.add(SyncRecord.builder()
                .externalId("PROD-002")
                .title("无线耳机 Pro")
                .content("Pro 无线耳机：主动降噪、续航 30 小时、蓝牙 5.3。")
                .tags(Map.of("category", "electronics", "price", "899"))
                .updatedAt(now)
                .build());
        records.add(SyncRecord.builder()
                .externalId("PROD-003")
                .title("智能音箱 Mini")
                .content("Mini 智能音箱：AI 助手、360° 音效、支持多房间。")
                .tags(Map.of("category", "smart-home", "price", "499"))
                .updatedAt(now)
                .build());
        records.add(SyncRecord.builder()
                .externalId("PROD-004")
                .title("机械键盘 K2")
                .content("K2 机械键盘：红轴、RGB 背光、Type-C 接线。")
                .tags(Map.of("category", "computer", "price", "699"))
                .updatedAt(now)
                .build());
        records.add(SyncRecord.builder()
                .externalId("PROD-005")
                .title("4K 显示器 M27")
                .content("M27 4K 显示器：27 寸 IPS、99% sRGB、HDR400。")
                .tags(Map.of("category", "computer", "price", "2499"))
                .updatedAt(now)
                .build());
        return records;
    }
}
