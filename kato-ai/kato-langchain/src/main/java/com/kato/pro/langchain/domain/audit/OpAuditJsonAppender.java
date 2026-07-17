package com.kato.pro.langchain.domain.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.config.AuditJsonProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 操作审计 JSON 文件 appender（spec §6 M14）。
 *
 * 输出 NDJSON（每行一条 JSON）到 {@code logs/op-audit.json}，供 ELK / Loki / filebeat 抓取。
 *
 * 关键设计：
 *   - 写失败仅 log warn，不抛（审计不能影响主业务）
 *   - 单进程 synchronized 串行写，多进程需要 filebeat 拉取（v1 不做分布式锁）
 *   - 字段顺序固定（LinkedHashMap）便于人眼 + grep
 *   - 首次 append 时延迟创建文件
 *   - Spring shutdown 时 flush + close
 */
@Slf4j
@Component
public class OpAuditJsonAppender {

    private final AuditJsonProperties properties;
    private final ObjectMapper objectMapper;

    /** lazy：首次 append 时初始化；null 表示 enabled=false 或未启动 */
    private BufferedWriter writer;

    public OpAuditJsonAppender(AuditJsonProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void init() {
        if (!properties.isEnabled()) {
            log.info("OpAuditJsonAppender disabled (kato.audit.file.enabled=false)");
            return;
        }
        // enabled 但还未触发 append，先不创建文件（避免空文件）
        log.info("OpAuditJsonAppender enabled, target path={}, maxLineLength={}",
                properties.getPath(), properties.getMaxLineLength());
    }

    @PreDestroy
    void shutdown() {
        synchronized (this) {
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    log.warn("OpAuditJsonAppender close failed: {}", e.getMessage());
                } finally {
                    writer = null;
                }
            }
        }
    }

    /**
     * 追加一条审计事件为 NDJSON 行。失败仅 log warn，不抛。
     *
     * @param audit 已写 DB 的审计实体
     */
    public void append(OpAudit audit) {
        if (!properties.isEnabled()) return;
        if (audit == null) return;

        String line;
        try {
            line = toJsonLine(audit);
        } catch (Exception e) {
            log.warn("OpAuditJsonAppender.toJsonLine failed: {}", e.getMessage());
            return;
        }

        // 超长截断（防一行写满 inode）
        int maxLen = properties.getMaxLineLength();
        if (line.length() > maxLen) {
            line = line.substring(0, maxLen - 3) + "...";
        }

        synchronized (this) {
            try {
                ensureWriter();
                if (writer == null) return;  // 创建失败已经在 ensureWriter 里 log
                writer.write(line);
                writer.newLine();
                writer.flush();  // v1 每行 flush，简单可靠；批写优化留给 M14.1
            } catch (IOException e) {
                log.warn("OpAuditJsonAppender write failed: path={}, err={}",
                        properties.getPath(), e.getMessage());
            }
        }
    }

    private void ensureWriter() throws IOException {
        if (writer != null) return;
        Path target = Paths.get(properties.getPath());
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * 序列化 OpAudit 为单行 JSON（NDJSON）。
     * 字段顺序固定，便于 grep。
     */
    private String toJsonLine(OpAudit a) throws JsonProcessingException {
        // LinkedHashMap 保证字段顺序
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ts", a.getCreateTime() == null ? null : a.getCreateTime().toString());
        m.put("tenantId", a.getTenantId());
        m.put("userId", a.getUserId());
        m.put("username", a.getUsername());
        m.put("action", a.getAction());
        m.put("resource", a.getResource());
        m.put("resourceId", a.getResourceId());
        m.put("requestUri", a.getRequestUri());
        m.put("httpMethod", a.getHttpMethod());
        m.put("httpStatus", a.getHttpStatus());
        m.put("traceId", a.getTraceId());
        m.put("durationMs", a.getDurationMs());
        m.put("args", a.getArgsJson());
        m.put("result", a.getResultJson());
        return objectMapper.writeValueAsString(m);
    }
}
