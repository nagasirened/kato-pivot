package com.kato.pro.langchain.infrastructure.external.minimax;

import com.kato.pro.langchain.common.exception.SystemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M2 沙箱测试：用 ExchangeFunction stub 拦截 HTTP 请求（不发起真实网络）。
 */
class MiniMaxClientTest {

    private MiniMaxClient client;
    private AtomicReference<org.springframework.web.reactive.function.client.ClientRequest> capturedRequest;
    private String stubResponseBody;
    private HttpStatus stubStatus;
    private String stubContentType;

    @BeforeEach
    void setup() {
        capturedRequest = new AtomicReference<>();
        stubResponseBody = """
            {
              "id": "cmpl-1",
              "model": "m2.5",
              "choices": [{
                "index": 0,
                "message": {"role": "assistant", "content": "你好！"},
                "finish_reason": "stop"
              }],
              "usage": {"prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15}
            }
            """;
        stubStatus = HttpStatus.OK;
        stubContentType = MediaType.APPLICATION_JSON_VALUE;

        MiniMaxProperties props = new MiniMaxProperties();
        props.setApiKey("test-key");
        props.setBaseUrl("http://stub");
        props.setReadTimeoutMs(5_000);

        ExchangeFunction stubFn = request -> {
            capturedRequest.set(request);
            return Mono.just(
                    ClientResponse.create(stubStatus)
                            .header(HttpHeaders.CONTENT_TYPE, stubContentType)
                            .body(stubResponseBody)
                            .build());
        };

        WebClient webClient = WebClient.builder()
                .baseUrl("http://stub")
                .exchangeFunction(stubFn)
                .build();

        client = new MiniMaxClient(webClient, props);
    }

    @Test
    void chatCompletions_parsesContentFromFirstChoice() {
        String result = client.chatCompletions("m2.5", "你是助手", "你好").block();
        assertEquals("你好！", result);

        // 验证请求
        var req = capturedRequest.get();
        assertNotNull(req);
        assertEquals("POST", req.method().name());
        assertTrue(req.url().toString().endsWith("/chat/completions"),
                "URL should be /chat/completions, got: " + req.url());
        assertEquals("Bearer test-key",
                req.headers().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void chatCompletions_5xxError_mapsToUpstreamError() {
        stubStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        stubResponseBody = "internal error";

        SystemException ex = assertThrows(SystemException.class,
                () -> client.chatCompletions("m2.5", "sys", "hi").block());
        assertTrue(ex.getMessage().contains("500"),
                "ex should mention 500, got: " + ex.getMessage());
    }

    @Test
    void chatCompletions_emptyChoices_throwsUpstreamError() {
        stubResponseBody = """
            {"id": "x", "model": "m2.5", "choices": [], "usage": null}
            """;
        SystemException ex = assertThrows(SystemException.class,
                () -> client.chatCompletions("m2.5", "sys", "hi").block());
        assertTrue(ex.getMessage().contains("empty choices"));
    }

    @Test
    void chatCompletions_4xxError_propagates() {
        stubStatus = HttpStatus.UNAUTHORIZED;
        stubResponseBody = "{\"error\": \"invalid api key\"}";

        SystemException ex = assertThrows(SystemException.class,
                () -> client.chatCompletions("m2.5", "sys", "hi").block());
        assertTrue(ex.getMessage().contains("401"));
    }
}
