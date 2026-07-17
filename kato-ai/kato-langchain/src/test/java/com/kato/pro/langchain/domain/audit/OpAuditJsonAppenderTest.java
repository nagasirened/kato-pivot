package com.kato.pro.langchain.domain.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.config.AuditJsonProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpAuditJsonAppender 单元测试（spec §6 M14）。
 *
 * 覆盖：
 *   - 文件写入 + NDJSON 单行格式
 *   - 字段顺序固定
 *   - 超长截断
 *   - enabled=false 时不写入
 *   - null 入参安全
 *   - 并发写不撕裂
 */
class OpAuditJsonAppenderTest {

    @TempDir
    Path tempDir;

    private AuditJsonProperties props;
    private ObjectMapper objectMapper;
    private OpAuditJsonAppender appender;

    @BeforeEach
    void setUp() {
        props = new AuditJsonProperties();
        props.setEnabled(true);
        props.setPath(tempDir.resolve("op-audit.json").toString());
        props.setMaxLineLength(8000);
        objectMapper = new ObjectMapper();
        appender = new OpAuditJsonAppender(props, objectMapper);
        appender.init();
    }

    @AfterEach
    void tearDown() {
        appender.shutdown();
    }

    private OpAudit sampleAudit() {
        OpAudit a = new OpAudit();
        a.setTenantId(1L);
        a.setUserId(1001L);
        a.setUsername("alice");
        a.setAction("TRIGGER");
        a.setResource("SYNC");
        a.setResourceId("42");
        a.setRequestUri("/api/v1/admin/sync/foo/trigger");
        a.setHttpMethod("POST");
        a.setHttpStatus(200);
        a.setTraceId("trace-abc");
        a.setDurationMs(123);
        a.setArgsJson("{\"k\":\"v\"}");
        a.setResultJson("{\"ok\":true}");
        a.setCreateTime(LocalDateTime.of(2026, 7, 16, 13, 0, 0));
        return a;
    }

    @Test
    void append_writesOneNdjsonLine() throws Exception {
        appender.append(sampleAudit());
        appender.shutdown();

        List<String> lines = Files.readAllLines(Path.of(props.getPath()));
        assertEquals(1, lines.size(), "should write exactly one line");
        assertTrue(lines.get(0).startsWith("{") && lines.get(0).endsWith("}"),
                "line should be a JSON object, got: " + lines.get(0));
    }

    @Test
    void append_fieldOrderIsStable() throws Exception {
        appender.append(sampleAudit());
        appender.shutdown();

        String line = Files.readString(Path.of(props.getPath())).strip();
        // LinkedHashMap 保证字段顺序：ts,tenantId,userId,username,action,...
        int idxTs = line.indexOf("\"ts\"");
        int idxTenant = line.indexOf("\"tenantId\"");
        int idxUser = line.indexOf("\"userId\"");
        int idxAction = line.indexOf("\"action\"");
        int idxResult = line.indexOf("\"result\"");
        assertTrue(idxTs < idxTenant);
        assertTrue(idxTenant < idxUser);
        assertTrue(idxUser < idxAction);
        assertTrue(idxAction < idxResult);
    }

    @Test
    void append_includesAllKeyFields() throws Exception {
        appender.append(sampleAudit());
        appender.shutdown();

        String line = Files.readString(Path.of(props.getPath()));
        assertTrue(line.contains("\"username\":\"alice\""));
        assertTrue(line.contains("\"action\":\"TRIGGER\""));
        assertTrue(line.contains("\"resource\":\"SYNC\""));
        assertTrue(line.contains("\"httpStatus\":200"));
        assertTrue(line.contains("\"traceId\":\"trace-abc\""));
        assertTrue(line.contains("\"durationMs\":123"));
    }

    @Test
    void append_overlongLineIsTruncatedWithEllipsis() throws Exception {
        props.setMaxLineLength(50);
        appender = new OpAuditJsonAppender(props, objectMapper);
        appender.init();
        appender.append(sampleAudit());
        appender.shutdown();

        String line = Files.readString(Path.of(props.getPath())).strip();
        assertTrue(line.length() <= 50, "line should be truncated to <= maxLineLength, got len=" + line.length());
        assertTrue(line.endsWith("..."), "truncated line should end with ..., got: " + line);
    }

    @Test
    void append_disabled_doesNothing() throws Exception {
        props.setEnabled(false);
        appender = new OpAuditJsonAppender(props, objectMapper);
        appender.init();
        appender.append(sampleAudit());
        appender.shutdown();

        // 文件可能不存在或为空
        Path target = Path.of(props.getPath());
        if (Files.exists(target)) {
            assertEquals(0, Files.size(target), "disabled appender should not write any content");
        }
    }

    @Test
    void append_nullAudit_isSafe() {
        assertDoesNotThrow(() -> appender.append(null));
        appender.shutdown();
    }

    @Test
    void append_concurrentWrites_produceIntactLines() throws Exception {
        int threads = 8;
        int writesPerThread = 25;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int t = 0; t < threads; t++) {
            int tid = t;
            pool.submit(() -> {
                for (int i = 0; i < writesPerThread; i++) {
                    OpAudit a = sampleAudit();
                    a.setUserId((long) tid);
                    a.setResourceId("t" + tid + "-" + i);
                    appender.append(a);
                }
            });
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "writes should complete");
        appender.shutdown();

        List<String> lines = Files.readAllLines(Path.of(props.getPath()));
        assertEquals(threads * writesPerThread, lines.size(),
                "expected " + threads * writesPerThread + " intact lines, got " + lines.size());

        // 每行必须可解析 JSON（不撕裂）
        for (String line : lines) {
            assertTrue(line.startsWith("{") && line.endsWith("}"),
                    "concurrent write produced torn line: " + line);
            // Jackson 解析必须成功
            try {
                objectMapper.readTree(line);
            } catch (Exception e) {
                fail("line is not valid JSON: " + line);
            }
        }
    }

    @Test
    void append_createsParentDirectory() throws Exception {
        Path nested = tempDir.resolve("a/b/c/op-audit.json");
        props.setPath(nested.toString());
        appender = new OpAuditJsonAppender(props, objectMapper);
        appender.init();
        appender.append(sampleAudit());
        appender.shutdown();

        assertTrue(Files.exists(nested), "nested parent dirs should be auto-created");
    }

    @Test
    void append_failedWrite_doesNotThrow() {
        // Linux /proc/self/ns 是只读目录，无法 createDirectories → 必 IO 失败
        props.setPath("/proc/self/ns/m14-test-audit.json");
        appender = new OpAuditJsonAppender(props, objectMapper);
        appender.init();
        assertDoesNotThrow(() -> appender.append(sampleAudit()),
                "write failure should be swallowed, not propagate");
        assertDoesNotThrow(() -> appender.shutdown());
    }
}
