package com.kato.pro.langchain.domain.sync;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.common.tenant.TenantInfo;
import com.kato.pro.langchain.common.trace.TraceContext;
import com.kato.pro.langchain.config.SyncProperties;
import com.kato.pro.langchain.domain.knowledge.KnowledgeDoc;
import com.kato.pro.langchain.domain.knowledge.KnowledgeDocService;
import com.kato.pro.langchain.infrastructure.persistence.SyncRunRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 同步应用服务（spec §6 M9）。
 *
 *   - @Scheduled(cron) → runAll()：遍历所有 SyncAdapter，按顺序执行
 *   - runOne(adapterName) → 手动触发单个 adapter
 *
 * 幂等：
 *   - externalId 同时作为 KnowledgeDoc.fileName + sourceId（保证同一业务记录映射到唯一 doc）
 *   - KnowledgeDocService.create 内部用 contentHash 去重（v1 提供幂等保证）
 *   - v2 计划加 unique index on (source_type, file_name) 在 DB 层兜底
 *
 * TraceId：
 *   - runOneInternal 入口 generate() 新 traceId，日志全链路可串联
 *   - 适配器 fetch 抛错时，traceId 已写日志便于排查
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncApplicationService {

    private final SyncProperties properties;
    private final List<SyncAdapter> adapters;
    private final KnowledgeDocService docService;
    private final SyncRunRecordMapper runMapper;

    /** 定时执行（cron from yml） */
    @Scheduled(cron = "${sync.cron:0 0 */2 * * *}")
    public void runAll() {
        if (!properties.isEnabled()) {
            log.info("Sync disabled, skip scheduled run");
            return;
        }
        log.info("Sync scheduled run started: {} adapters", adapters.size());
        for (SyncAdapter adapter : adapters) {
            try {
                runOneInternal(adapter, null);
            } catch (Exception e) {
                log.error("Sync adapter threw: name={}", adapter.name(), e);
            }
        }
        log.info("Sync scheduled run finished");
    }

    /** admin 手动触发 */
    public SyncRunRecord runOne(String adapterName) {
        SyncAdapter adapter = findAdapter(adapterName);
        return runOneInternal(adapter, null);
    }

    /** 暴露给测试：可注入 lastSyncTime */
    public SyncRunRecord runOneWithTime(String adapterName, Instant lastSyncTime) {
        SyncAdapter adapter = findAdapter(adapterName);
        return runOneInternal(adapter, lastSyncTime);
    }

    private SyncRunRecord runOneInternal(SyncAdapter adapter, Instant lastSyncTime) {
        String traceId = TraceContext.generate();
        Long tenantId = properties.getDefaultTenant();
        TenantContext.set(TenantInfo.of(tenantId, 0L));

        SyncRunRecord run = new SyncRunRecord();
        run.setTenantId(tenantId);
        run.setAdapterName(adapter.name());
        run.setStartTime(LocalDateTime.now());
        run.setSuccessCount(0);
        run.setFailCount(0);
        run.setStatus(SyncStatus.RUNNING.name());
        runMapper.insert(run);
        log.info("Sync started: adapter={}, runId={}", adapter.name(), run.getId());

        int success = 0, fail = 0;
        try {
            List<SyncRecord> records = adapter.fetchSince(lastSyncTime);
            log.info("Fetched {} records from {}", records.size(), adapter.name());
            for (SyncRecord rec : records) {
                try {
                    processOne(adapter, rec);
                    success++;
                } catch (Exception e) {
                    fail++;
                    log.error("Sync record failed: adapter={}, externalId={}",
                            adapter.name(), rec.getExternalId(), e);
                }
            }
            run.setSuccessCount(success);
            run.setFailCount(fail);
            run.setStatus(fail == 0 ? SyncStatus.SUCCESS.name() : SyncStatus.PARTIAL.name());
        } catch (Exception e) {
            log.error("Sync adapter threw: name={}", adapter.name(), e);
            run.setStatus(SyncStatus.FAILED.name());
            run.setErrorMsg(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
        run.setEndTime(LocalDateTime.now());
        runMapper.updateById(run);
        TenantContext.clear();
        TraceContext.clear();
        return run;
    }

    /** 单条 record 处理：调 KnowledgeDocService.create；contentHash 已提供去重幂等 */
    private void processOne(SyncAdapter adapter, SyncRecord rec) {
        byte[] content = rec.getContent() == null
                ? new byte[0]
                : rec.getContent().getBytes(StandardCharsets.UTF_8);
        KnowledgeDoc doc = docService.create(
                rec.getTitle(),
                rec.getExternalId(),
                "text/plain",
                content,
                adapter.sourceType(),
                rec.getExternalId());
        log.debug("Sync doc upsert: externalId={}, docId={}", rec.getExternalId(), doc.getId());
    }

    private SyncAdapter findAdapter(String name) {
        Optional<SyncAdapter> found = adapters.stream()
                .filter(a -> a.name().equalsIgnoreCase(name))
                .findFirst();
        return found.orElseThrow(() ->
                new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "sync adapter 不存在: " + name));
    }

    /** 查询最近运行记录 */
    public List<SyncRunRecord> recentRuns(String adapterName, int limit) {
        return runMapper.findRecentByAdapter(adapterName, limit);
    }

    /** 所有 adapter 状态概览 */
    public List<Map<String, Object>> statusOverview() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SyncAdapter a : adapters) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("adapterName", a.name());
            entry.put("description", a.description());
            entry.put("sourceType", a.sourceType().name());
            List<SyncRunRecord> recent = runMapper.findRecentByAdapter(a.name(), 1);
            entry.put("lastRun", recent.isEmpty() ? null : recent.get(0));
            result.add(entry);
        }
        return result;
    }
}
