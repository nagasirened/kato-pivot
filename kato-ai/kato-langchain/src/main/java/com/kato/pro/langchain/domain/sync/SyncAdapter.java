package com.kato.pro.langchain.domain.sync;

import java.time.Instant;
import java.util.List;

/**
 * 同步适配器 SPI（spec §6 D3）。
 *
 * 实现规则：
 *   - 必须 @Component
 *   - name() 返回 SyncSource.adapterName() 之一
 *   - sourceType() 返回对应的 SourceType
 *   - fetchSince(lastSyncTime) 返回该时间点之后的增量数据；lastSyncTime=null 返回全量
 *
 * 失败策略：
 *   - fetchSince 抛异常 → 整个 adapter 标记 FAILED
 *   - 单条 record 异常 → log + skip，继续下一条（syncApplicationService 兜底）
 */
public interface SyncAdapter {

    /** 适配器名（用于 controller 路由 + sync_run_record 关联） */
    String name();

    /** 对应 SourceType（写入 KnowledgeDoc.source_type） */
    com.kato.pro.langchain.domain.knowledge.SourceType sourceType();

    /** 自上次同步以来的增量数据；lastSyncTime=null 时返回全量 */
    List<SyncRecord> fetchSince(Instant lastSyncTime);

    /** 同步源描述（admin 查询用） */
    default String description() {
        SyncSource s = SyncSource.fromAdapterName(name());
        return s == null ? name() : s.description();
    }
}
