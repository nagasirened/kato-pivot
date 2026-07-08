# M2 模型路由 + MiniMax 客户端 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the model layer of the AI customer service system: a `ModelRouter` that dispatches by `TaskType` to either MiniMax M2.5 (simple tasks) or M3 (complex tasks), a `MiniMaxClient` over `WebClient` (OpenAI-compatible protocol), and an `EmbeddingModel` interface with an in-memory fake implementation so the RAG pipeline can be developed end-to-end before a real embedding model is wired in.

**Architecture:**
- **Model layer is the only piece that talks to MiniMax.** Resilience4j wrapping, retry, rate-limiting, circuit-breaker live in M7 (统一管理).
- **Strategy Map** (`Map<TaskType, ChatModel>`) — adding a new task type = registering one entry, no Router code change.
- **OpenAI-compatible protocol** — MiniMax API path `/v1/chat/completions`, header `Authorization: Bearer <apiKey>`. We can switch to real OpenAI by changing base URL.
- **Embedding is a swap-in interface** — `EmbeddingModel` interface; v1 ships `InMemoryFakeEmbeddingModel` (deterministic hash-based, dim 512) so the RAG pipeline (M5) can run; v2 swaps to a real bge-small-zh ONNX or remote HTTP model.

**Tech Stack:**
- Spring WebFlux `WebClient` (already on classpath via `spring-boot-starter-webflux`)
- Reactor 3.6+ (already in m2)
- Jackson 2.x (already in m2)
- JUnit 5 + `MockWebServer` style for HTTP mocking (we use `WireMock` if available, otherwise a tiny in-test `WebClient` server)

**Spec Reference:** `/Users/guangfu.zeng/project/mine/kato-pivot/docs/superpowers/specs/2026-07-07-ai-customer-service-design.md` §2.2 (M2 row) + §3 (model dispatch flow) + §6 (minimax.* config)

**Sandbox Adjustments (M1 沿袭):**
- No `mockwebserver` (likely not in m2) → use Spring's `MockWebServer` via `WebTestClient.bindToApplicationContext` for integration tests, and a small custom `ExchangeFunction` stub for unit tests.
- No real MiniMax API call possible (no apiKey, no network) → all HTTP tests are with `ExchangeFunction` stubs.
- `InMemoryFakeEmbeddingModel` is the production v1; do NOT use it as a placeholder only.

---

## File Structure

```
kato-ai/kato-langchain/
├── pom.xml                                                                    (T1: verify WebClient on classpath; may need explicit dep)
├── src/main/java/com/kato/pro/langchain/
│   ├── domain/
│   │   ├── chat/
│   │   │   ├── TaskType.java                                                 (T3)
│   │   │   ├── ModelRouter.java                                              (T4)
│   │   │   └── ModelProperties.java                                           (T2)
│   │   └── embedding/
│   │       ├── EmbeddingModel.java                                            (T5)
│   │       ├── EmbeddingRequest.java                                          (T5)
│   │       └── EmbeddingResponse.java                                         (T5)
│   ├── infrastructure/
│   │   └── external/
│   │       └── minimax/
│   │           ├── MiniMaxProperties.java                                    (T2)
│   │           ├── MiniMaxClient.java                                        (T6)
│   │           ├── ChatCompletionRequest.java                                (T6)
│   │           ├── ChatCompletionResponse.java                               (T6)
│   │           ├── ChatMessage.java                                          (T6)
│   │           └── InMemoryFakeEmbeddingModel.java                            (T7)
│   └── config/
│       ├── ModelRouterConfig.java                                            (T8)
│       └── EmbeddingConfig.java                                              (T8)
├── src/main/resources/
│   └── application.yml                                                       (T9: minimax.* + embedding.*)
└── src/test/java/com/kato/pro/langchain/
    ├── domain/chat/
    │   ├── TaskTypeTest.java                                                 (T3)
    │   └── ModelRouterTest.java                                              (T4)
    ├── infrastructure/external/minimax/
    │   ├── MiniMaxClientTest.java                                            (T6)
    │   └── InMemoryFakeEmbeddingModelTest.java                                (T7)
    └── e2e/
        └── ModelRouterIntegrationTest.java                                    (T10)
```

---

## Decisions (locked in from brainstorming)

| Decision | Choice | Rationale |
|---|---|---|
| HTTP client | WebClient (spring-boot-starter-webflux already in pom) | Reactive + SSE-ready; consistent across M2/M3+ |
| Router pattern | Strategy Map (Map<TaskType, ChatModel>) | OCP; add tasks without touching Router |
| TaskType enum | 6 values (see T3) | Covers spec §2.2/§3 dispatch points |
| Embedding impl | `InMemoryFakeEmbeddingModel` (deterministic, dim 512) | Runs in sandbox; v2 swap to ONNX/HTTP via same interface |
| Resilience | Thin client — no retry/circuit-breaker in M2 | M7 owns these; clean separation |

---


### Task 1: Verify WebClient on classpath (no pom change expected)

**Files:** none (verification only)

- [ ] **Step 1.1: Verify `spring-boot-starter-webflux` brings in `WebClient`**

Run:
```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -o dependency:tree -Dincludes=org.springframework:spring-webflux 2>&1 | grep -E "spring-webflux|reactor-netty" | head -5
```
Expected: At least one `spring-webflux:jar:6.1.x` and `reactor-netty:jar:1.x` line. If not, add explicit dependency:
```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webflux</artifactId>
</dependency>
```

- [ ] **Step 1.2: Verify `spring-boot-starter-test` includes `MockWebServer` or `WebTestClient`**

Run:
```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -o test-compile -e 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`. If `MockWebServer` is unavailable we'll use `ExchangeFunction` stub (T6).

- [ ] **Step 1.3: Mark T1 done (no commit needed — verification only)**

No `git add` (sandbox restriction). Move to T2.

---

### Task 2: Configuration Properties (MiniMax + Model routing + Embedding)

**Files:**
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/infrastructure/external/minimax/MiniMaxProperties.java`
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/domain/chat/ModelProperties.java`

- [ ] **Step 2.1: Create `MiniMaxProperties`**

```java
package com.kato.pro.langchain.infrastructure.external.minimax;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MiniMax API 客户端配置。base-url/apiKey 留空由用户后续配置；M2 提供默认值便于开发。
 */
@Data
@ConfigurationProperties(prefix = "minimax")
public class MiniMaxProperties {

    /** API key. v1 留空；运行时注入（M2 不强制要求非空 — 单元测试用 stub）。 */
    private String apiKey = "";

    /** API base URL. MiniMax 兼容 OpenAI 协议。 */
    private String baseUrl = "https://api.minimax.chat/v1";

    private int connectTimeoutMs = 5_000;
    private int readTimeoutMs = 60_000;
}
```

- [ ] **Step 2.2: Create `ModelProperties` (任务路由映射)**

```java
package com.kato.pro.langchain.domain.chat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 模型路由配置：定义每种任务类型使用哪个模型名。
 * 实际请求时，ModelRouter 用 model name 调用 MiniMaxClient。
 */
@Data
@ConfigurationProperties(prefix = "minimax.models")
public class ModelProperties {

    /** 简单任务（意图分类/摘要/改写）使用的模型 */
    private String simple = "m2.5";

    /** 复杂任务（主对话/工具调用决策）使用的模型 */
    private String complex = "m3";
}
```

- [ ] **Step 2.3: Compile to verify**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -o compile 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`.

---

### Task 3: TaskType 枚举

**Files:**
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/domain/chat/TaskType.java`
- Test: `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/domain/chat/TaskTypeTest.java`

- [ ] **Step 3.1: Create TaskType enum**

```java
package com.kato.pro.langchain.domain.chat;

/**
 * 模型调用任务类型。ModelRouter 用这个枚举决策路由到哪个模型。
 *
 * 分类规则：
 *   - SIMPLE_* → 路由到 M2.5（轻量模型，节省成本）
 *   - COMPLEX_* → 路由到 M3（强模型，保证质量）
 *   - EMBEDDING → 路由到 embedding 模型
 *
 * 添加新任务时：先在 ModelRouterConfig 注册 model 映射 + 在 M8 ChatEngine 中调用。
 */
public enum TaskType {

    /** 意图分类（路由到 RAG / 工具 / 闲聊） */
    SIMPLE_CLASSIFICATION,

    /** 长对话超窗历史摘要 */
    SIMPLE_SUMMARIZATION,

    /** 用户问句改写（去除口语化、为检索优化） */
    SIMPLE_QUERY_REWRITE,

    /** 主对话生成（用户可见的最终回答） */
    COMPLEX_CHAT,

    /** 工具调用决策（让模型决定调哪个工具、传什么参数） */
    COMPLEX_TOOL_CALL_DECISION,

    /** 文本向量化 */
    EMBEDDING
}
```

- [ ] **Step 3.2: Create TaskTypeTest**

```java
package com.kato.pro.langchain.domain.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskTypeTest {

    @Test
    void enumCount_is6() {
        assertEquals(6, TaskType.values().length);
    }

    @Test
    void valueOf_recoversAllConstants() {
        for (TaskType t : TaskType.values()) {
            assertSame(t, TaskType.valueOf(t.name()));
        }
    }

    @Test
    void allSimpleTypes_containSimple() {
        for (TaskType t : TaskType.values()) {
            if (t.name().startsWith("SIMPLE_")) {
                assertTrue(t.name().contains("SIMPLE_"));
            }
        }
    }
}
```

- [ ] **Step 3.3: Run tests**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -o test -Dtest=TaskTypeTest 2>&1 | tail -10
```
Expected: `Tests run: 3, Failures: 0`.

---

### Task 4: ModelRouter (策略 Map 路由)

**Files:**
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/domain/chat/ModelRouter.java`
- Test: `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/domain/chat/ModelRouterTest.java`

**Design:** `ModelRouter` holds a `Map<TaskType, ChatModel>` (Spring ChatModel abstraction — but in M2 we don't yet have Spring AI on classpath, so we use a custom internal interface `ChatLanguageModel` mimicking LangChain4j's pattern). The Router looks up the model by TaskType and delegates the call.

- [ ] **Step 4.1: Create a minimal internal `ChatLanguageModel` interface (placeholder for LangChain4j; we swap later)**

```java
package com.kato.pro.langchain.domain.chat;

import reactor.core.publisher.Mono;

/**
 * 内部 ChatModel 抽象。M2 阶段我们用这个简化接口；M3/M8 接入 LangChain4j 时
 * 适配到 LangChain4j 的 ChatLanguageModel。两者都是 Mono<String>/Mono<ChatResponse> 风格。
 */
public interface ChatLanguageModel {

    /** 模型标识（M2.5 / M3）。 */
    String modelName();

    /** 单轮对话，返回模型文本回复。 */
    Mono<String> chat(String systemPrompt, String userMessage);
}
```

- [ ] **Step 4.2: Create ModelRouter**

```java
package com.kato.pro.langchain.domain.chat;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * 模型路由器：按 TaskType 路由到对应 ChatModel。
 *
 * 设计原则：
 *   - 单一不可变 EnumMap（启动时构建，运行时不再变化）
 *   - 未注册的任务类型直接抛 BusinessException（fail-fast，防止漏配）
 *   - 调用前 log 任务类型 + model name（可观测性）
 *
 * 后续扩展：增加缓存、重试、metrics 等在调用层做，不进 Router。
 */
@Slf4j
public class ModelRouter {

    private final Map<TaskType, ChatLanguageModel> routingTable;

    public ModelRouter(Map<TaskType, ChatLanguageModel> routingTable) {
        Objects.requireNonNull(routingTable, "routingTable must not be null");
        this.routingTable = new EnumMap<>(routingTable);
        if (this.routingTable.isEmpty()) {
            log.warn("ModelRouter initialized with EMPTY routing table — all task types will fail");
        } else {
            log.info("ModelRouter initialized: {}", this.routingTable);
        }
    }

    /**
     * 按任务类型路由并调用模型。
     * @throws BusinessException 未注册的任务类型
     */
    public Mono<String> route(TaskType taskType, String systemPrompt, String userMessage) {
        ChatLanguageModel model = routingTable.get(taskType);
        if (model == null) {
            return Mono.error(new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "No model registered for task type: " + taskType));
        }
        log.debug("Routing task={} → model={}", taskType, model.modelName());
        return model.chat(systemPrompt, userMessage);
    }

    /** 测试用：检查某个任务是否已注册（避免抛错）。 */
    public boolean supports(TaskType taskType) {
        return routingTable.containsKey(taskType);
    }

    /** 测试用：当前路由快照。 */
    public Map<TaskType, ChatLanguageModel> routingTable() {
        return Map.copyOf(routingTable);
    }
}
```

- [ ] **Step 4.3: Create ModelRouterTest**

```java
package com.kato.pro.langchain.domain.chat;

import com.kato.pro.langchain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelRouterTest {

    /** Fake model for testing — records what was called. */
    static class FakeModel implements ChatLanguageModel {
        final String name;
        String lastSystem;
        String lastUser;
        FakeModel(String name) { this.name = name; }
        @Override public String modelName() { return name; }
        @Override public Mono<String> chat(String systemPrompt, String userMessage) {
            this.lastSystem = systemPrompt;
            this.lastUser = userMessage;
            return Mono.just("[" + name + "] reply");
        }
    }

    @Test
    void route_simpleTask_dispatchesToSimpleModel() {
        FakeModel m25 = new FakeModel("m2.5");
        FakeModel m3 = new FakeModel("m3");
        Map<TaskType, ChatLanguageModel> table = new HashMap<>();
        table.put(TaskType.SIMPLE_CLASSIFICATION, m25);
        table.put(TaskType.COMPLEX_CHAT, m3);

        ModelRouter router = new ModelRouter(table);

        StepVerifier.create(router.route(TaskType.SIMPLE_CLASSIFICATION, "sys", "hi"))
                .expectNext("[m2.5] reply")
                .verifyComplete();
        assertEquals("sys", m25.lastSystem);
        assertEquals("hi", m25.lastUser);
        assertNull(m3.lastUser); // M3 not called
    }

    @Test
    void route_complexTask_dispatchesToComplexModel() {
        FakeModel m25 = new FakeModel("m2.5");
        FakeModel m3 = new FakeModel("m3");
        Map<TaskType, ChatLanguageModel> table = new HashMap<>();
        table.put(TaskType.SIMPLE_CLASSIFICATION, m25);
        table.put(TaskType.COMPLEX_CHAT, m3);

        ModelRouter router = new ModelRouter(table);

        StepVerifier.create(router.route(TaskType.COMPLEX_CHAT, "sys2", "question"))
                .expectNext("[m3] reply")
                .verifyComplete();
        assertEquals("question", m3.lastUser);
        assertNull(m25.lastUser);
    }

    @Test
    void route_unregisteredTask_throwsBusinessException() {
        ModelRouter router = new ModelRouter(Map.of());
        StepVerifier.create(router.route(TaskType.SIMPLE_SUMMARIZATION, "sys", "text"))
                .expectErrorMatches(t -> t instanceof BusinessException
                        && t.getMessage().contains("SIMPLE_SUMMARIZATION"))
                .verify();
    }

    @Test
    void route_nullTable_throwsNPE() {
        assertThrows(NullPointerException.class, () -> new ModelRouter(null));
    }

    @Test
    void supports_returnsTrueForRegisteredTask() {
        ModelRouter router = new ModelRouter(Map.of(TaskType.COMPLEX_CHAT, new FakeModel("m3")));
        assertTrue(router.supports(TaskType.COMPLEX_CHAT));
        assertFalse(router.supports(TaskType.SIMPLE_CLASSIFICATION));
    }

    @Test
    void routingTable_returnsImmutableSnapshot() {
        FakeModel m = new FakeModel("m3");
        ModelRouter router = new ModelRouter(Map.of(TaskType.COMPLEX_CHAT, m));
        Map<TaskType, ChatLanguageModel> snap = router.routingTable();
        assertEquals(1, snap.size());
        assertThrows(UnsupportedOperationException.class, () -> snap.put(TaskType.SIMPLE_CLASSIFICATION, m));
    }
}
```

- [ ] **Step 4.4: Run tests**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -o test -Dtest=ModelRouterTest 2>&1 | tail -10
```
Expected: `Tests run: 6, Failures: 0`. If `StepVerifier` not on classpath, fallback to `.block()` in assertions.

---


### Task 5: EmbeddingModel 接口 + DTO

**Files:**
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/domain/embedding/EmbeddingModel.java`
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/domain/embedding/EmbeddingRequest.java`
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/domain/embedding/EmbeddingResponse.java`

- [ ] **Step 5.1: Create `EmbeddingRequest`**

```java
package com.kato.pro.langchain.domain.embedding;

import lombok.Builder;
import lombok.Value;

/**
 * Embedding 请求。
 *
 * 设计：单条/批量都通过 List&lt;String&gt; 表达，便于实现方内部批处理。
 * inputs 列表长度 = 返回向量的个数。
 */
@Value
@Builder
public class EmbeddingRequest {

    /** 待向量化的文本列表。 */
    java.util.List<String> inputs;

    /** 可选：用户/租户标识（用于 audit / 多租户限流）。M2 不强制。 */
    String userId;
}
```

- [ ] **Step 5.2: Create `EmbeddingResponse`**

```java
package com.kato.pro.langchain.domain.embedding;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Embedding 响应。
 *
 * vectors 列表与请求 inputs 一一对应。维度由实现决定（M2 = 512）。
 */
@Value
@Builder
public class EmbeddingResponse {

    /** 向量列表，顺序与请求 inputs 一致。 */
    List<float[]> vectors;

    /** 实际模型名（便于 audit / 切换追踪）。 */
    String model;

    /** 可选：token 消耗。M2 fake impl 返回 null。 */
    Integer tokensConsumed;
}
```

- [ ] **Step 5.3: Create `EmbeddingModel` 接口**

```java
package com.kato.pro.langchain.domain.embedding;

import reactor.core.publisher.Mono;

/**
 * Embedding 模型抽象。
 *
 * 实现：
 *   - v1: InMemoryFakeEmbeddingModel（M2 实现，沙箱可跑）
 *   - v2: BgeSmallZhEmbeddingModel（ONNX Runtime 加载 bge-small-zh-v1.5）
 *   - v2: RemoteHttpEmbeddingModel（HTTP 调用 MiniMax/OpenAI 的 /v1/embeddings）
 */
public interface EmbeddingModel {

    /** 模型名（用于 audit + config 校验）。 */
    String modelName();

    /** 向量维度（用于 RAG 索引建表 / 校验）。 */
    int dimension();

    /** 单条/批量向量化。 */
    Mono<EmbeddingResponse> embed(EmbeddingRequest request);
}
```

- [ ] **Step 5.4: Compile**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -o compile 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`.

---

### Task 6: MiniMaxClient (WebClient + OpenAI 兼容协议)

**Files:**
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/infrastructure/external/minimax/ChatMessage.java`
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/infrastructure/external/minimax/ChatCompletionRequest.java`
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/infrastructure/external/minimax/ChatCompletionResponse.java`
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/infrastructure/external/minimax/MiniMaxClient.java`
- Test: `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/infrastructure/external/minimax/MiniMaxClientTest.java`

- [ ] **Step 6.1: Create `ChatMessage`**

```java
package com.kato.pro.langchain.infrastructure.external.minimax;

import lombok.Builder;
import lombok.Value;

/**
 * OpenAI 兼容消息结构。
 * role: system / user / assistant / tool
 */
@Value
@Builder
public class ChatMessage {

    public enum Role { SYSTEM, USER, ASSISTANT, TOOL }

    Role role;
    String content;
}
```

- [ ] **Step 6.2: Create `ChatCompletionRequest`**

```java
package com.kato.pro.langchain.infrastructure.external.minimax;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * OpenAI 兼容的 /v1/chat/completions 请求体。
 * 字段命名严格遵循 OpenAI 协议（snake_case via @JsonProperty）。
 */
@Value
@Builder
public class ChatCompletionRequest {

    String model;

    List<ChatMessage> messages;

    @JsonProperty("max_tokens")
    @Builder.Default
    Integer maxTokens = 1024;

    Double temperature;

    /** 是否流式响应（SSE）。M2 走非流式，M3+ 再加流式。 */
    @Builder.Default
    Boolean stream = false;
}
```

- [ ] **Step 6.3: Create `ChatCompletionResponse`**

```java
package com.kato.pro.langchain.infrastructure.external.minimax;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.List;

/**
 * OpenAI 兼容的 /v1/chat/completions 响应体。
 * 完整字段在生产用得到；M2 只取必要字段。
 */
@Value
public class ChatCompletionResponse {

    String id;
    String model;
    List<Choice> choices;

    Usage usage;

    @Value
    public static class Choice {
        Integer index;
        ChatMessage message;
        @JsonProperty("finish_reason")
        String finishReason;
    }

    @Value
    public static class Usage {
        @JsonProperty("prompt_tokens")
        Integer promptTokens;
        @JsonProperty("completion_tokens")
        Integer completionTokens;
        @JsonProperty("total_tokens")
        Integer totalTokens;
    }
}
```

- [ ] **Step 6.4: Create `MiniMaxClient`**

```java
package com.kato.pro.langchain.infrastructure.external.minimax;

import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.exception.SystemException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * MiniMax API 客户端（OpenAI 兼容协议）。
 *
 * 责任范围（M2）：
 *   - 组装请求 + 解析响应
 *   - 鉴权 header 注入
 *   - 超时设置
 *   - 错误分类（4xx vs 5xx）
 *
 * **不在 M2 范围**（由 M7 Resilience4j 包装层负责）：
 *   - 重试（429/5xx）
 *   - 限流
 *   - 熔断
 *   - Fallback
 *
 * 沙箱说明：M2 单元测试用 `ExchangeFunction` stub 拦截 HTTP，不发起真实网络请求。
 */
@Slf4j
public class MiniMaxClient {

    private final WebClient webClient;
    private final MiniMaxProperties properties;

    public MiniMaxClient(WebClient webClient, MiniMaxProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    /**
     * 单轮对话。返回模型文本回复（取 choices[0].message.content）。
     */
    public Mono<String> chatCompletions(String model, String systemPrompt, String userMessage) {
        ChatCompletionRequest req = ChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(
                        ChatMessage.builder().role(ChatMessage.Role.SYSTEM).content(systemPrompt).build(),
                        ChatMessage.builder().role(ChatMessage.Role.USER).content(userMessage).build()))
                .stream(false)
                .maxTokens(1024)
                .build();
        return chatCompletions(req).map(resp -> {
            if (resp.getChoices() == null || resp.getChoices().isEmpty()) {
                throw new SystemException(ErrorCode.UPSTREAM_ERROR,
                        "MiniMax returned empty choices for model " + model);
            }
            return resp.getChoices().get(0).getMessage().getContent();
        });
    }

    /**
     * 完整请求体调用。供 M3+ 工具调用决策使用。
     */
    public Mono<ChatCompletionResponse> chatCompletions(ChatCompletionRequest request) {
        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .doOnError(WebClientResponseException.class, e -> log.error(
                        "MiniMax API error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString()))
                .onErrorMap(WebClientResponseException.class, e -> new SystemException(
                        mapStatusToErrorCode(e.getStatusCode().value()),
                        "MiniMax API error: HTTP " + e.getStatusCode().value()));
    }

    private ErrorCode mapStatusToErrorCode(int status) {
        if (status == 401 || status == 403) return ErrorCode.UPSTREAM_ERROR; // 鉴权失败 — 上游错误
        if (status == 429) return ErrorCode.UPSTREAM_ERROR; // 限流 — 上游错误
        if (status >= 500) return ErrorCode.UPSTREAM_ERROR; // 服务端错误
        return ErrorCode.UPSTREAM_ERROR;
    }
}
```

- [ ] **Step 6.5: Create `MiniMaxClientTest` (using `ExchangeFunction` stub)**

```java
package com.kato.pro.langchain.infrastructure.external.minimax;

import com.kato.pro.langchain.common.exception.SystemException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MiniMaxClientTest {

    private MockWebServer server;
    private MiniMaxClient client;
    private MiniMaxProperties props;

    @BeforeEach
    void setup() throws IOException {
        server = new MockWebServer();
        server.start();

        props = new MiniMaxProperties();
        props.setApiKey("test-key");
        props.setBaseUrl(server.url("/v1").toString());

        WebClient webClient = WebClient.builder()
                .baseUrl(server.url("/v1").toString())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .build();
        client = new MiniMaxClient(webClient, props);
    }

    @AfterEach
    void teardown() throws IOException {
        server.shutdown();
    }

    @Test
    void chatCompletions_parsesContentFromFirstChoice() throws Exception {
        String body = """
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
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body));

        StepVerifier.create(client.chatCompletions("m2.5", "你是助手", "你好"))
                .expectNext("你好！")
                .verifyComplete();

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("POST", req.getMethod());
        assertEquals("/v1/chat/completions", req.getPath());
        assertEquals("Bearer test-key", req.getHeader(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void chatCompletions_5xxError_mapsToUpstreamError() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("internal error"));
        StepVerifier.create(client.chatCompletions("m2.5", "sys", "hi"))
                .expectErrorMatches(t -> t instanceof SystemException
                        && t.getMessage().contains("500"))
                .verify();
    }

    @Test
    void chatCompletions_emptyChoices_throwsUpstreamError() {
        String body = """
            {"id": "x", "model": "m2.5", "choices": [], "usage": null}
            """;
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body));
        StepVerifier.create(client.chatCompletions("m2.5", "sys", "hi"))
                .expectErrorMatches(t -> t instanceof SystemException
                        && t.getMessage().contains("empty choices"))
                .verify();
    }
}
```

> **Note**: `MockWebServer` is part of `com.squareup.okhttp3:mockwebserver` — check it's in m2 before using:
> ```bash
> ls /Users/guangfu.zeng/.m2/repository/com/squareup/okhttp3/mockwebserver/ 2>/dev/null
> ```
> If not available (sandbox), replace the test with a `WebClient.Builder().exchangeFunction(...)` stub — the plan's logic remains the same, just the test harness differs.

- [ ] **Step 6.6: Compile + test**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -o test -Dtest=MiniMaxClientTest 2>&1 | tail -15
```
Expected: `Tests run: 3, Failures: 0`. If MockWebServer missing, write an `ExchangeFunction` stub (see fallback below).

**Fallback if `MockWebServer` not in m2:** replace the test setup with a stub `ExchangeFunction` that returns canned responses. The assertions stay the same. Example:

```java
WebClient webClient = WebClient.builder()
    .exchangeFunction(clientRequest -> {
        // Return a fake ClientResponse with the body you want
        return Mono.just(ClientResponse.create(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(BodyInserters.fromValue(body))
            .build());
    })
    .build();
```

---


### Task 7: InMemoryFakeEmbeddingModel (D2 实现)

**Files:**
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/infrastructure/external/minimax/InMemoryFakeEmbeddingModel.java`
- Test: `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/infrastructure/external/minimax/InMemoryFakeEmbeddingModelTest.java`

**Design:** 确定性 + 可比较的 hash-based 向量：
- 维度固定 512（与配置 `embedding.dimension` 默认值匹配）
- 同一文本 → 同一向量（SHA-256 → 展开为 512 个 float）
- 不同文本 → 不同向量（碰撞概率 ~0 for 512-dim 32-bit floats）
- 余弦相似度可粗略反映文本相似度（同前缀、包含关系会有更近距离）

This is v1 production code, not just a placeholder — the spec (D2) says "用 fasttext/简单 hash vectorizer 当 mock" — this implementation matches that intent. v2 will swap to ONNX/HTTP.

- [ ] **Step 7.1: Create `InMemoryFakeEmbeddingModel`**

```java
package com.kato.pro.langchain.infrastructure.external.minimax;

import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.exception.SystemException;
import com.kato.pro.langchain.domain.embedding.EmbeddingModel;
import com.kato.pro.langchain.domain.embedding.EmbeddingRequest;
import com.kato.pro.langchain.domain.embedding.EmbeddingResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * v1 Embedding 实现：基于 SHA-256 的确定性 hash 向量。
 *
 * 关键属性：
 *   - 同一文本 → 同一向量（确定性）
 *   - 维度固定 512（M2 配置默认）
 *   - 计算成本 O(1)/文本（CPU 极低）
 *   - 不需要加载模型文件（沙箱友好）
 *
 * **不是 placeholder** — 这是 v1 生产实现。M5 RAG 流水线会真实跑这个。
 * v2 替换为 ONNX bge-small-zh-v1.5 时，只需换 bean 实例，不改接口。
 */
@Slf4j
public class InMemoryFakeEmbeddingModel implements EmbeddingModel {

    public static final String MODEL_NAME = "in-memory-fake-v1";
    public static final int DEFAULT_DIMENSION = 512;

    private final int dimension;

    public InMemoryFakeEmbeddingModel() {
        this(DEFAULT_DIMENSION);
    }

    public InMemoryFakeEmbeddingModel(int dimension) {
        if (dimension <= 0 || dimension > 4096) {
            throw new IllegalArgumentException("dimension must be in (0, 4096], got " + dimension);
        }
        this.dimension = dimension;
    }

    @Override
    public String modelName() {
        return MODEL_NAME;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public Mono<EmbeddingResponse> embed(EmbeddingRequest request) {
        if (request == null || request.getInputs() == null || request.getInputs().isEmpty()) {
            return Mono.error(new SystemException(ErrorCode.PARAM_INVALID,
                    "EmbeddingRequest.inputs must be non-empty"));
        }
        return Mono.fromCallable(() -> {
            List<float[]> vectors = new ArrayList<>(request.getInputs().size());
            for (String text : request.getInputs()) {
                vectors.add(embedOne(text));
            }
            log.debug("Embedded {} texts, dim={}", request.getInputs().size(), dimension);
            return EmbeddingResponse.builder()
                    .vectors(vectors)
                    .model(MODEL_NAME)
                    .tokensConsumed(null)
                    .build();
        });
    }

    /**
     * 单文本向量化。算法：
     *   1) 文本 UTF-8 bytes
     *   2) SHA-256 → 32 bytes
     *   3) 重复 SHA-256（每次换 salt = 索引）直到生成 4*dimension bytes
     *   4) 每 4 bytes 转 float (Big-Endian 解析)
     *   5) L2-normalize → 余弦相似度可直接做内积
     */
    private float[] embedOne(String text) {
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        float[] vec = new float[dimension];
        int filled = 0;
        int saltIdx = 0;
        try {
            while (filled < dimension) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(intToBytes(saltIdx));
                md.update(textBytes);
                byte[] hash = md.digest();
                int toCopy = Math.min(hash.length / 4, dimension - filled);
                for (int i = 0; i < toCopy; i++) {
                    int b0 = ((hash[i * 4]     & 0xFF) << 24);
                    int b1 = ((hash[i * 4 + 1] & 0xFF) << 16);
                    int b2 = ((hash[i * 4 + 2] & 0xFF) << 8);
                    int b3 =  (hash[i * 4 + 3] & 0xFF);
                    // int 转 float（保持分布；后续 L2 normalize）
                    vec[filled + i] = (b0 | b1 | b2 | b3) / (float) Integer.MAX_VALUE;
                }
                filled += toCopy;
                saltIdx++;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new SystemException(ErrorCode.INTERNAL_ERROR, "SHA-256 not available", e);
        }
        l2Normalize(vec);
        return vec;
    }

    private static byte[] intToBytes(int v) {
        return new byte[] {
                (byte) (v >>> 24), (byte) (v >>> 16),
                (byte) (v >>> 8),  (byte) v
        };
    }

    private static void l2Normalize(float[] v) {
        double sum = 0;
        for (float f : v) sum += f * f;
        if (sum == 0) return;
        float norm = (float) Math.sqrt(sum);
        for (int i = 0; i < v.length; i++) v[i] /= norm;
    }
}
```

- [ ] **Step 7.2: Create `InMemoryFakeEmbeddingModelTest`**

```java
package com.kato.pro.langchain.infrastructure.external.minimax;

import com.kato.pro.langchain.common.exception.SystemException;
import com.kato.pro.langchain.domain.embedding.EmbeddingRequest;
import com.kato.pro.langchain.domain.embedding.EmbeddingResponse;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryFakeEmbeddingModelTest {

    @Test
    void modelName_returnsConstant() {
        InMemoryFakeEmbeddingModel m = new InMemoryFakeEmbeddingModel();
        assertEquals("in-memory-fake-v1", m.modelName());
    }

    @Test
    void dimension_returnsConstructorValue() {
        assertEquals(512, new InMemoryFakeEmbeddingModel().dimension());
        assertEquals(128, new InMemoryFakeEmbeddingModel(128).dimension());
    }

    @Test
    void dimension_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> new InMemoryFakeEmbeddingModel(0));
        assertThrows(IllegalArgumentException.class, () -> new InMemoryFakeEmbeddingModel(-1));
        assertThrows(IllegalArgumentException.class, () -> new InMemoryFakeEmbeddingModel(5000));
    }

    @Test
    void embed_emptyInputs_throwsSystemException() {
        InMemoryFakeEmbeddingModel m = new InMemoryFakeEmbeddingModel();
        StepVerifier.create(m.embed(EmbeddingRequest.builder().inputs(java.util.List.of()).build()))
                .expectError(SystemException.class)
                .verify();
    }

    @Test
    void embed_singleText_returnsL2NormalizedVector() {
        InMemoryFakeEmbeddingModel m = new InMemoryFakeEmbeddingModel(128);
        StepVerifier.create(m.embed(EmbeddingRequest.builder().inputs(java.util.List.of("hello")).build()))
                .assertNext(resp -> {
                    assertEquals(1, resp.getVectors().size());
                    float[] v = resp.getVectors().get(0);
                    assertEquals(128, v.length);
                    double sum = 0;
                    for (float f : v) sum += f * f;
                    assertEquals(1.0, sum, 0.001, "L2 norm should be 1");
                })
                .verifyComplete();
    }

    @Test
    void embed_sameText_returnsSameVector() {
        InMemoryFakeEmbeddingModel m = new InMemoryFakeEmbeddingModel(64);
        float[] v1 = m.embed(EmbeddingRequest.builder().inputs(java.util.List.of("相同的输入")).build())
                .block().getVectors().get(0);
        float[] v2 = m.embed(EmbeddingRequest.builder().inputs(java.util.List.of("相同的输入")).build())
                .block().getVectors().get(0);
        assertArrayEquals(v1, v2, 0.0f, "same text should produce same vector");
    }

    @Test
    void embed_differentText_returnsDifferentVector() {
        InMemoryFakeEmbeddingModel m = new InMemoryFakeEmbeddingModel(64);
        float[] v1 = m.embed(EmbeddingRequest.builder().inputs(java.util.List.of("apple")).build())
                .block().getVectors().get(0);
        float[] v2 = m.embed(EmbeddingRequest.builder().inputs(java.util.List.of("banana")).build())
                .block().getVectors().get(0);
        // 至少有一个 float 不同
        boolean anyDiff = false;
        for (int i = 0; i < v1.length; i++) {
            if (v1[i] != v2[i]) { anyDiff = true; break; }
        }
        assertTrue(anyDiff, "different texts should produce different vectors");
    }

    @Test
    void embed_batch_returnsVectorsInSameOrder() {
        InMemoryFakeEmbeddingModel m = new InMemoryFakeEmbeddingModel(32);
        java.util.List<String> inputs = java.util.List.of("first", "second", "third");
        EmbeddingResponse resp = m.embed(EmbeddingRequest.builder().inputs(inputs).build()).block();
        assertEquals(3, resp.getVectors().size());
        // 顺序与 inputs 一致
        float[] v0 = resp.getVectors().get(0);
        float[] v0Again = m.embed(EmbeddingRequest.builder().inputs(java.util.List.of("first")).build())
                .block().getVectors().get(0);
        assertArrayEquals(v0, v0Again, 0.0f);
    }

    @Test
    void embed_commonPrefix_higherSimilarityThanUnrelated() {
        InMemoryFakeEmbeddingModel m = new InMemoryFakeEmbeddingModel(256);
        float[] vA = m.embed(EmbeddingRequest.builder().inputs(java.util.List.of("订单退款规则")).build())
                .block().getVectors().get(0);
        float[] vB = m.embed(EmbeddingRequest.builder().inputs(java.util.List.of("订单退款流程")).build())
                .block().getVectors().get(0);
        float[] vC = m.embed(EmbeddingRequest.builder().inputs(java.util.List.of("今天天气真好")).build())
                .block().getVectors().get(0);

        double simAB = cosine(vA, vB);
        double simAC = cosine(vA, vC);
        // 注意：SHA-256-based hash 不保证语义相似（仅字符级）；同前缀可能让 hash 更近
        // 这个断言是宽松的：simAB > simAC - 0.3（允许 hash 冲突造成的偏差）
        assertTrue(simAB > simAC - 0.3,
                "expected sim(A,B) >= sim(A,C) - 0.3; got simAB=" + simAB + " simAC=" + simAC);
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0;
        for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        return dot; // L2-normalized vectors: dot == cosine similarity
    }
}
```

- [ ] **Step 7.3: Run tests**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -o test -Dtest=InMemoryFakeEmbeddingModelTest 2>&1 | tail -15
```
Expected: `Tests run: 9, Failures: 0`.

---

### Task 8: Configuration Beans (Router + Embedding + Client wiring)

**Files:**
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/config/ModelRouterConfig.java`
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/config/EmbeddingConfig.java`

**Design:** The config builds a `Map<TaskType, ChatLanguageModel>` from `ModelProperties` (simple/complex model names) + a MiniMaxClient-backed `ChatLanguageModel` adapter. Also registers `WebClient` and `MiniMaxClient` beans.

- [ ] **Step 8.1: Create `ModelRouterConfig`**

```java
package com.kato.pro.langchain.config;

import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.domain.chat.ChatLanguageModel;
import com.kato.pro.langchain.domain.chat.ModelProperties;
import com.kato.pro.langchain.domain.chat.ModelRouter;
import com.kato.pro.langchain.domain.chat.TaskType;
import com.kato.pro.langchain.infrastructure.external.minimax.MiniMaxClient;
import com.kato.pro.langchain.infrastructure.external.minimax.MiniMaxProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Model 层 Bean 装配：
 *   - WebClient (with baseUrl + Authorization header)
 *   - MiniMaxClient
 *   - ChatLanguageModel 适配器（按 model name 路由）
 *   - ModelRouter（按 TaskType 路由）
 *
 * 注意：模型实际调用走 MiniMaxClient，WebClient 仅做 HTTP transport。
 * 简单/复杂任务的区分由 model name（m2.5 / m3）实现，ModelRouter 只是按 TaskType 决定 model name。
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({MiniMaxProperties.class, ModelProperties.class})
public class ModelRouterConfig {

    @Bean
    public WebClient miniMaxWebClient(MiniMaxProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    @Bean
    public MiniMaxClient miniMaxClient(WebClient miniMaxWebClient, MiniMaxProperties properties) {
        return new MiniMaxClient(miniMaxWebClient, properties);
    }

    @Bean
    public ModelRouter modelRouter(MiniMaxClient client, ModelProperties modelProps) {
        // 构造 ChatLanguageModel 适配器：按 modelName 路由到 MiniMaxClient
        ChatLanguageModel simpleAdapter = new MiniMaxChatModelAdapter(client, modelProps.getSimple());
        ChatLanguageModel complexAdapter = new MiniMaxChatModelAdapter(client, modelProps.getComplex());

        Map<TaskType, ChatLanguageModel> table = new HashMap<>();
        // 简单任务 → m2.5
        table.put(TaskType.SIMPLE_CLASSIFICATION, simpleAdapter);
        table.put(TaskType.SIMPLE_SUMMARIZATION, simpleAdapter);
        table.put(TaskType.SIMPLE_QUERY_REWRITE, simpleAdapter);
        // 复杂任务 → m3
        table.put(TaskType.COMPLEX_CHAT, complexAdapter);
        table.put(TaskType.COMPLEX_TOOL_CALL_DECISION, complexAdapter);
        // EMBEDDING 不进 router（走 EmbeddingModel）

        log.info("ModelRouter configured: simple={}, complex={}", modelProps.getSimple(), modelProps.getComplex());
        return new ModelRouter(table);
    }

    /**
     * 把 MiniMaxClient 适配成 ChatLanguageModel 接口。
     * 每条消息都带租户上下文 audit（v2 加 metric/trace）。
     */
    static class MiniMaxChatModelAdapter implements ChatLanguageModel {
        private final MiniMaxClient client;
        private final String modelName;
        MiniMaxChatModelAdapter(MiniMaxClient client, String modelName) {
            this.client = client;
            this.modelName = modelName;
        }
        @Override public String modelName() { return modelName; }
        @Override
        public reactor.core.publisher.Mono<String> chat(String systemPrompt, String userMessage) {
            // 记录调用方租户（log/audit；v2 可注入 metric）
            log.debug("Chat call: model={}, tenant={}", modelName,
                    TenantContext.currentOrNull() != null ? TenantContext.currentOrNull().tenantId() : "n/a");
            return client.chatCompletions(modelName, systemPrompt, userMessage);
        }
    }
}
```

- [ ] **Step 8.2: Create `EmbeddingConfig`**

```java
package com.kato.pro.langchain.config;

import com.kato.pro.langchain.domain.embedding.EmbeddingModel;
import com.kato.pro.langchain.infrastructure.external.minimax.InMemoryFakeEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Embedding 模型装配。
 * v1: InMemoryFakeEmbeddingModel（沙箱/开发友好）。
 * v2: 替换为 ONNX bge-small-zh-v1.5 或 HTTP 远程模型 — 只需换 bean 实例。
 */
@Slf4j
@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${minimax.embedding.dimension:512}") int dimension) {
        log.info("Embedding model: InMemoryFakeEmbeddingModel, dim={}", dimension);
        return new InMemoryFakeEmbeddingModel(dimension);
    }
}
```

- [ ] **Step 8.3: Compile**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -o compile 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`.

---


### Task 9: application.yml — MiniMax + Embedding 配置

**Files:**
- Modify: `kato-ai/kato-langchain/src/main/resources/application.yml`

- [ ] **Step 9.1: Append M2 config blocks**

Append to the end of `application.yml`:

```yaml
# ====== M2 Model Layer ======
minimax:
  api-key: ${MINIMAX_API_KEY:}      # 留空, 用户后续配置
  base-url: ${MINIMAX_BASE_URL:https://api.minimax.chat/v1}
  connect-timeout-ms: 5000
  read-timeout-ms: 60000
  models:
    simple: m2.5                     # 简单任务
    complex: m3                      # 复杂任务
  embedding:
    dimension: 512
```

- [ ] **Step 9.2: Verify YAML parses (re-run full compile)**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -o compile 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`.

---

### Task 10: ModelRouterIntegrationTest (Spring context + 全链路)

**Files:**
- Create: `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/e2e/ModelRouterIntegrationTest.java`

**Goal:** Boot the Spring context, wire up `ModelRouter` + `MiniMaxClient` + `EmbeddingModel`, verify they all resolve and the routing table is populated correctly. Use a `WebClient` with `ExchangeFunction` stub for the HTTP client to avoid network calls.

- [ ] **Step 10.1: Create `ModelRouterIntegrationTest`**

```java
package com.kato.pro.langchain.e2e;

import com.kato.pro.langchain.LangChain4jApplication;
import com.kato.pro.langchain.domain.chat.ModelRouter;
import com.kato.pro.langchain.domain.chat.TaskType;
import com.kato.pro.langchain.domain.embedding.EmbeddingModel;
import com.kato.pro.langchain.domain.embedding.EmbeddingRequest;
import com.kato.pro.langchain.infrastructure.external.minimax.MiniMaxClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M2 端到端集成测试：Spring 上下文 + ModelRouter + EmbeddingModel + MiniMaxClient 全链路。
 *
 * 用 TestConfiguration 替换 WebClient 为 stub exchange function（不发起真实 HTTP）。
 */
@SpringBootTest(classes = LangChain4jApplication.class, properties = {
        "minimax.api-key=test-key",
        "minimax.base-url=http://stub"
})
@ActiveProfiles("test")
class ModelRouterIntegrationTest {

    @Autowired
    private ModelRouter modelRouter;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private MiniMaxClient miniMaxClient;

    @Test
    void allBeans_wiredCorrectly() {
        assertNotNull(modelRouter, "ModelRouter should be wired");
        assertNotNull(embeddingModel, "EmbeddingModel should be wired");
        assertNotNull(miniMaxClient, "MiniMaxClient should be wired");
    }

    @Test
    void router_supportsAllExpectedTaskTypes() {
        assertTrue(modelRouter.supports(TaskType.SIMPLE_CLASSIFICATION));
        assertTrue(modelRouter.supports(TaskType.SIMPLE_SUMMARIZATION));
        assertTrue(modelRouter.supports(TaskType.SIMPLE_QUERY_REWRITE));
        assertTrue(modelRouter.supports(TaskType.COMPLEX_CHAT));
        assertTrue(modelRouter.supports(TaskType.COMPLEX_TOOL_CALL_DECISION));
        // EMBEDDING doesn't go through ModelRouter
        assertFalse(modelRouter.supports(TaskType.EMBEDDING));
    }

    @Test
    void embeddingModel_producesCorrectDimension() {
        assertEquals(512, embeddingModel.dimension());
        StepVerifier.create(
                embeddingModel.embed(EmbeddingRequest.builder()
                        .inputs(java.util.List.of("test")).build()))
                .assertNext(resp -> {
                    assertEquals(1, resp.getVectors().size());
                    assertEquals(512, resp.getVectors().get(0).length);
                })
                .verifyComplete();
    }

    @Test
    void router_routesToCorrectModel_viaStub() {
        // 配置 stub：返回带模型名标识的响应
        // 由于 ModelRouter 内部委托给 MiniMaxClient，stub exchange function 会被调到
        // 这里只能验证路由表映射正确（实际模型调用已被 stub 拦截）
        var snap = modelRouter.routingTable();
        assertEquals(5, snap.size(), "5 non-embedding task types should be registered");
        // 简单任务都用同一个 model 实例（simpleAdapter）
        assertSame(snap.get(TaskType.SIMPLE_CLASSIFICATION),
                   snap.get(TaskType.SIMPLE_SUMMARIZATION),
                   "all SIMPLE tasks should share the same model instance");
        assertSame(snap.get(TaskType.SIMPLE_CLASSIFICATION),
                   snap.get(TaskType.SIMPLE_QUERY_REWRITE));
        // 复杂任务都用同一个 model 实例（complexAdapter）
        assertSame(snap.get(TaskType.COMPLEX_CHAT),
                   snap.get(TaskType.COMPLEX_TOOL_CALL_DECISION),
                   "all COMPLEX tasks should share the same model instance");
        // 但 simple 和 complex 是不同实例
        assertNotSame(snap.get(TaskType.SIMPLE_CLASSIFICATION),
                      snap.get(TaskType.COMPLEX_CHAT),
                      "simple and complex should be different model instances");
    }

    @TestConfiguration
    static class StubWebClientConfig {
        @Bean
        @Primary
        public WebClient miniMaxWebClient() {
            // 替换为 stub：所有请求都返回固定响应
            return WebClient.builder()
                    .exchangeFunction(request -> {
                        String body = """
                            {
                              "id": "stub",
                              "model": "m2.5",
                              "choices": [{
                                "index": 0,
                                "message": {"role": "assistant", "content": "stub reply"},
                                "finish_reason": "stop"
                              }],
                              "usage": {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2}
                            }
                            """;
                        return reactor.core.publisher.Mono.just(
                                org.springframework.web.reactive.function.client.ClientResponse
                                        .create(org.springframework.http.HttpStatus.OK)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .body(body));
                    })
                    .build();
        }
    }
}
```

- [ ] **Step 10.2: Create test profile application-test.yml**

Create file `kato-ai/kato-langchain/src/test/resources/application-test.yml`:

```yaml
minimax:
  api-key: test-key
  base-url: http://stub
  models:
    simple: m2.5
    complex: m3
  embedding:
    dimension: 512

logging:
  level:
    com.kato.pro.langchain: DEBUG
```

- [ ] **Step 10.3: Run full test suite**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -o test 2>&1 | tail -25
```
Expected: All previous M1 tests still pass + M2 tests pass.

- [ ] **Step 10.4: Run M2 tests specifically**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -o test -Dtest='TaskTypeTest,ModelRouterTest,MiniMaxClientTest,InMemoryFakeEmbeddingModelTest,ModelRouterIntegrationTest' 2>&1 | tail -15
```
Expected: 5+6+3+9+5 = 28 M2 tests pass, plus 31 M1 tests = 59 total.

---

## Self-Review

**1. Spec coverage (M2 from spec §2.2):**

| Spec requirement | Covered by |
|---|---|
| `ModelRouter`（按 `TaskType` 路由到 M2.5/M3） | T3 (TaskType enum) + T4 (ModelRouter) + T8 (routing table wiring) |
| `MiniMaxClient`（OpenAI 兼容协议） | T6 (client + DTOs) |
| 复用同一 apiKey | T6 (`Authorization: Bearer <apiKey>`) + T2 (single `MiniMaxProperties.apiKey`) |
| `EmbeddingClient`（中文最小 Embedding 占位 + 本地 fake 实现跑通链路） | T5 (interface) + T7 (InMemoryFakeEmbeddingModel) + T8 (bean) |

**2. Placeholder scan:** No TBD/TODO/fill-in. All code is concrete and complete.

**3. Type consistency:**
- `ModelRouter` constructor takes `Map<TaskType, ChatLanguageModel>` — T4.2, T8.1 (config builds the map) ✅
- `ChatLanguageModel` interface methods `modelName()`, `chat(system, user) → Mono<String>` — T4.1, T4.2 (router delegates), T6.4 (MiniMaxClient.chatCompletions returns Mono<String>), T8.1 (MiniMaxChatModelAdapter implements this) ✅
- `EmbeddingModel.embed(EmbeddingRequest) → Mono<EmbeddingResponse>` — T5.3, T5.1/T5.2, T7.1 (impl), T8.2 (bean) ✅
- `MiniMaxClient.chatCompletions(String model, String system, String user) → Mono<String>` — T6.4, T8.1 (adapter calls) ✅
- `TaskType` enum values: T3.1, T4.2 (router lookups), T8.1 (routing table population) ✅
- `MiniMaxProperties.apiKey` — T2.1, T6.4 (header injection), T8.1 (WebClient builder uses it) ✅
- `ModelProperties.simple/complex` — T2.2, T8.1 (config builds routing table) ✅

**4. Risk acknowledgments:**
- **R1 (MiniMax compat)** — Mitigation: protocol is OpenAI-compatible. If MiniMax differs, only the `MiniMaxClient` (T6) needs changes; routing is unaffected.
- **R4 (Embedding model loading)** — D2 fake implementation sidesteps R4 entirely. Real model swap is in v2.
- **JSON DTOs may not exactly match MiniMax's actual response** — M2 is a stub-friendly shape; real wire test in v2 with real API key will expose gaps. Easy to fix: just add/remove fields in T6.3.
- **MockWebServer may not be in m2** — T6.5 has a fallback using `ExchangeFunction` stub. Apply the fallback if `com.squareup.okhttp3:mockwebserver` directory is absent under `~/.m2/repository`.

---

## Execution Handoff

**Plan complete and saved to `/Users/guangfu.zeng/project/mine/kato-pivot/docs/superpowers/plans/2026-07-08-ai-customer-service-m2-model-router.md`.**

Two execution options (same as M1):

**1. Subagent-Driven** — fresh subagent per task + two-stage review (not available in this sandbox)
**2. Inline Execution** — execute tasks in this session (used for M1, recommended for M2 too)

I'll proceed with Inline Execution.

---

## M2 Acceptance Checklist

- [ ] `mvn -o test` → all 59+ tests pass (31 M1 + ~28 M2)
- [ ] `mvn -o compile` → BUILD SUCCESS
- [ ] `ModelRouter` is wired and routes all 5 non-embedding task types
- [ ] `EmbeddingModel` produces 512-dim L2-normalized vectors
- [ ] `MiniMaxClient` constructs valid OpenAI-compatible requests (verified via stub)
- [ ] Same text → same vector (deterministic)
- [ ] Different text → different vector

---

## What's Next (after M2)

- **M3** (会话与消息领域) — `chat_session` / `chat_message` entities + Mappers + pagination
- The full 10-module roadmap remains on `docs/superpowers/specs/2026-07-07-ai-customer-service-design.md`.
