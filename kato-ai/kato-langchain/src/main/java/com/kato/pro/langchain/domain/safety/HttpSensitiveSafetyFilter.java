package com.kato.pro.langchain.domain.safety;

import com.kato.pro.langchain.config.SafetyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * HTTP filter（生产连 kato-sensitive-client；R7 故障降级）。
 *
 * 实现要点：
 *   - WebClient 调 POST /sensitive/check
 *   - 超时（timeoutMs）→ 降级 passFallback
 *   - HTTP 4xx/5xx → 降级 passFallback
 *   - 异常（连接拒绝/超时） → 降级 passFallback
 *
 * v1 简化：只关心 hasSensitive；sanitizedText 透传。
 */
@Slf4j
public class HttpSensitiveSafetyFilter implements ContentSafetyFilter {

    private final SafetyProperties properties;
    private final WebClient client;

    public HttpSensitiveSafetyFilter(SafetyProperties properties) {
        this(properties, defaultClient(properties));
    }

    public HttpSensitiveSafetyFilter(SafetyProperties properties, WebClient client) {
        this.properties = properties;
        this.client = client;
    }

    @Override
    public String name() { return "http-sensitive"; }

    @Override
    public SafetyResult check(String text, SafetyContext ctx) {
        if (!properties.isEnabled() || !properties.getSensitiveClient().isEnabled()) {
            return SafetyResult.pass();
        }
        if (text == null || text.isEmpty()) return SafetyResult.pass();

        try {
            Map<String, Object> req = Map.of(
                    "text", text,
                    "fuzzyMatch", false,
                    "mode", "ALL",
                    "sanitize", true,
                    "maskChar", "*",
                    "wordType", "ALL");
            @SuppressWarnings("rawtypes")
            Map resp = client.post()
                    .uri("/sensitive/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(properties.getSensitiveClient().getTimeoutMs()))
                    .onErrorResume(e -> Mono.empty())
                    .block();

            if (resp == null) {
                return SafetyResult.passFallback("http-null-or-timeout");
            }
            boolean hasSensitive = Boolean.TRUE.equals(resp.get("hasSensitive"));
            if (hasSensitive) {
                String sanitized = (String) resp.get("sanitizedText");
                Object matches = resp.get("matches");
                List<String> hits = extractHits(matches);
                log.warn("HttpSensitiveSafetyFilter rejected: hits={} dir={} traceId={}",
                        hits, ctx.getDirection(), ctx.getTraceId());
                return SafetyResult.builder()
                        .passed(false).hitWords(hits).sanitizedText(sanitized).fallback(false)
                        .details(Map.of("filter", name(), "direction",
                                ctx.getDirection() == null ? "" : ctx.getDirection().name()))
                        .build();
            }
            return SafetyResult.pass();
        } catch (Exception e) {
            log.warn("HttpSensitiveSafetyFilter degraded: dir={} traceId={} err={}",
                    ctx.getDirection(), ctx.getTraceId(), e.toString());
            return SafetyResult.passFallback(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractHits(Object matches) {
        if (!(matches instanceof List)) return List.of();
        List<String> hits = new java.util.ArrayList<>();
        for (Object m : (List<Object>) matches) {
            if (m instanceof Map) {
                Object w = ((Map<String, Object>) m).get("word");
                if (w != null) hits.add(w.toString());
            }
        }
        return hits;
    }

    private static WebClient defaultClient(SafetyProperties p) {
        return WebClient.builder()
                .baseUrl(p.getSensitiveClient().getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }
}
