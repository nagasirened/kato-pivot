package com.kato.pro.langchain.e2e;

import com.kato.pro.langchain.domain.chat.ChatLanguageModel;
import com.kato.pro.langchain.domain.chat.ModelProperties;
import com.kato.pro.langchain.domain.chat.ModelRouter;
import com.kato.pro.langchain.domain.chat.TaskType;
import com.kato.pro.langchain.domain.embedding.EmbeddingModel;
import com.kato.pro.langchain.domain.embedding.EmbeddingRequest;
import com.kato.pro.langchain.infrastructure.external.minimax.InMemoryFakeEmbeddingModel;
import com.kato.pro.langchain.infrastructure.external.minimax.MiniMaxClient;
import com.kato.pro.langchain.infrastructure.external.minimax.MiniMaxProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M2 端到端集成测试（沙箱友好版）：
 *   - 不用 @SpringBootTest（避免 Mockito + byte-buddy 自附加问题）
 *   - 手动 wire bean：WebClient stub + MiniMaxClient + ModelRouter + EmbeddingModel
 *   - 覆盖 Router + Embedding + Client 的关键集成点
 */
class ModelRouterIntegrationTest {

    private ModelRouter modelRouter;
    private EmbeddingModel embeddingModel;
    private MiniMaxClient miniMaxClient;
    private java.util.concurrent.atomic.AtomicReference<String> lastResponse;

    @BeforeEach
    void setup() {
        // 共享响应体
        lastResponse = new java.util.concurrent.atomic.AtomicReference<>("");

        MiniMaxProperties props = new MiniMaxProperties();
        props.setApiKey("test-key");
        props.setBaseUrl("http://stub");
        props.setReadTimeoutMs(5_000);

        WebClient webClient = WebClient.builder()
                .exchangeFunction(req -> {
                    String body = "{"
                            + "\"id\":\"stub\","
                            + "\"model\":\"m2.5\","
                            + "\"choices\":[{"
                            + "  \"index\":0,"
                            + "  \"message\":{\"role\":\"assistant\",\"content\":\"stub reply for "
                            + req.method().name() + " " + req.url().getPath() + "\"},"
                            + "  \"finish_reason\":\"stop\""
                            + "}],"
                            + "\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":1,\"total_tokens\":2}"
                            + "}";
                    lastResponse.set(body);
                    return Mono.just(
                            ClientResponse.create(HttpStatus.OK)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .body(body)
                                    .build());
                })
                .build();
        miniMaxClient = new MiniMaxClient(webClient, props);

        // 手动构造 ModelRouter
        ModelProperties modelProps = new ModelProperties();
        ChatLanguageModel simpleAdapter = new TestAdapter(miniMaxClient, modelProps.getSimple());
        ChatLanguageModel complexAdapter = new TestAdapter(miniMaxClient, modelProps.getComplex());

        Map<TaskType, ChatLanguageModel> table = new java.util.HashMap<>();
        table.put(TaskType.SIMPLE_CLASSIFICATION, simpleAdapter);
        table.put(TaskType.SIMPLE_SUMMARIZATION, simpleAdapter);
        table.put(TaskType.SIMPLE_QUERY_REWRITE, simpleAdapter);
        table.put(TaskType.COMPLEX_CHAT, complexAdapter);
        table.put(TaskType.COMPLEX_TOOL_CALL_DECISION, complexAdapter);
        modelRouter = new ModelRouter(table);

        embeddingModel = new InMemoryFakeEmbeddingModel(512);
    }

    @Test
    void allBeans_wiredCorrectly() {
        assertNotNull(modelRouter);
        assertNotNull(embeddingModel);
        assertNotNull(miniMaxClient);
    }

    @Test
    void router_supportsAllExpectedTaskTypes() {
        assertTrue(modelRouter.supports(TaskType.SIMPLE_CLASSIFICATION));
        assertTrue(modelRouter.supports(TaskType.SIMPLE_SUMMARIZATION));
        assertTrue(modelRouter.supports(TaskType.SIMPLE_QUERY_REWRITE));
        assertTrue(modelRouter.supports(TaskType.COMPLEX_CHAT));
        assertTrue(modelRouter.supports(TaskType.COMPLEX_TOOL_CALL_DECISION));
        assertFalse(modelRouter.supports(TaskType.EMBEDDING));
    }

    @Test
    void embeddingModel_producesCorrectDimension() {
        assertEquals(512, embeddingModel.dimension());
        var resp = embeddingModel.embed(
                EmbeddingRequest.builder().inputs(List.of("test")).build()).block();
        assertNotNull(resp);
        assertEquals(1, resp.getVectors().size());
        assertEquals(512, resp.getVectors().get(0).length);
    }

    @Test
    void router_routingTable_correctGrouping() {
        var snap = modelRouter.routingTable();
        assertEquals(5, snap.size());
        assertSame(snap.get(TaskType.SIMPLE_CLASSIFICATION),
                   snap.get(TaskType.SIMPLE_SUMMARIZATION));
        assertSame(snap.get(TaskType.COMPLEX_CHAT),
                   snap.get(TaskType.COMPLEX_TOOL_CALL_DECISION));
        assertNotSame(snap.get(TaskType.SIMPLE_CLASSIFICATION),
                      snap.get(TaskType.COMPLEX_CHAT));
    }

    @Test
    void router_endToEnd_returnsStubReply() {
        String result = modelRouter.route(TaskType.SIMPLE_CLASSIFICATION, "sys", "hi").block();
        assertNotNull(result);
        assertTrue(result.contains("stub reply"), "should contain stub content, got: " + result);
        // 验证请求真的发出去了
        assertTrue(lastResponse.get().contains("choices"));
    }

    /** Test-only ChatLanguageModel adapter (mirrors the one in ModelRouterConfig). */
    static class TestAdapter implements ChatLanguageModel {
        private final MiniMaxClient client;
        private final String modelName;
        TestAdapter(MiniMaxClient client, String modelName) {
            this.client = client;
            this.modelName = modelName;
        }
        @Override public String modelName() { return modelName; }
        @Override public reactor.core.publisher.Mono<String> chat(String systemPrompt, String userMessage) {
            return client.chatCompletions(modelName, systemPrompt, userMessage);
        }
    }
}
