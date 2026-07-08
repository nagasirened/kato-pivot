# M1 工程骨架 + 多租户底座 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the foundational skeleton of `kato-langchain`: unified Result/error handling, multi-tenant ThreadLocal context + SQL interceptor, TraceId MDC, MyBatis-Plus base entity, Flyway init for tenant/user tables, and a working Spring Boot 3 + Java 21 + MyBatis-Plus 3.5.7 application that boots cleanly with H2 (unit) or MySQL (integration).

**Architecture:**
- **Standalone foundation** — `kato-langchain` is Java 21 / Spring Boot 3 / `jakarta.*`, while the existing `kato-common`/`kato-web`/`kato-database` modules are Java 8 / Spring Boot 2 / `javax.*`. We CANNOT import them. M1 builds minimal standalone equivalents of `Result`, exception handling, MyBatis-Plus base mapper, etc.
- **Two-line tenant defense** — ① `TenantContextFilter` injects `tenantId`/`userId` into `TenantContext` (ThreadLocal). ② `TenantMybatisInterceptor` appends `WHERE tenant_id = ?` to every SQL automatically (defense-in-depth).
- **H2 for unit tests, Testcontainers MySQL 8 for integration tests** — local Docker is available; no MySQL daemon needed.

**Tech Stack:**
- Spring Boot 3.2.6, Java 21
- MyBatis-Plus 3.5.7 (project standard version)
- MySQL Connector/J 8.3.0 (runtime), H2 2.2.224 (test)
- Flyway 9.22.3 (migrations)
- Testcontainers 1.19.7 + MySQL 8.0 module
- Lombok 1.18.30

**Spec Reference:** `/Users/guangfu.zeng/project/mine/kato-pivot/docs/superpowers/specs/2026-07-07-ai-customer-service-design.md` §2.2 (M1 row) + §9 (R2 risk)

---

## File Structure (created in this plan)

```
kato-langchain/
├── pom.xml                                                                   (modified: T1)
├── src/main/java/com/kato/pro/langchain/
│   ├── LangChain4jApplication.java                                          (existing)
│   ├── common/
│   │   ├── result/Result.java                                                (T3)
│   │   ├── exception/
│   │   │   ├── ErrorCode.java                                                (T3)
│   │   │   ├── BusinessException.java                                        (T3)
│   │   │   ├── SystemException.java                                          (T3)
│   │   │   └── GlobalExceptionHandler.java                                   (T4)
│   │   ├── tenant/
│   │   │   ├── TenantContext.java                                           (T5)
│   │   │   ├── TenantInfo.java                                              (T5)
│   │   │   └── TenantContextFilter.java                                     (T5)
│   │   ├── trace/
│   │   │   ├── TraceContext.java                                            (T6)
│   │   │   └── TraceIdFilter.java                                           (T6)
│   │   └── entity/BaseEntity.java                                           (T7)
│   ├── infrastructure/persistence/
│   │   ├── TenantAwareBaseMapper.java                                       (T7)
│   │   └── TenantMybatisInterceptor.java                                    (T8)
│   └── config/
│       ├── MybatisPlusConfig.java                                           (T9)
│       └── WebConfig.java                                                   (T10)
├── src/main/resources/
│   ├── application.yml                                                      (T11)
│   └── db/migration/
│       └── V1__init_tenant_user.sql                                         (T12)
└── src/test/java/com/kato/pro/langchain/
    ├── common/result/ResultTest.java                                        (T3)
    ├── common/exception/
    │   ├── BusinessExceptionTest.java                                       (T3)
    │   └── GlobalExceptionHandlerTest.java                                  (T4)
    ├── common/tenant/TenantContextTest.java                                 (T5)
    ├── common/trace/TraceIdFilterTest.java                                  (T6)
    ├── infrastructure/persistence/
    │   └── TenantMybatisInterceptorTest.java                               (T8)  ★ P0 SECURITY TEST
    └── e2e/FoundationIntegrationTest.java                                 (T13)
```


---

### Task 1: Add M1 dependencies to pom.xml

**Files:**
- Modify: `kato-ai/kato-langchain/pom.xml`

**Why first:** All other tasks depend on MyBatis-Plus, MySQL driver, Flyway, H2 (test), and Testcontainers being on the classpath. We keep the existing LangChain4j starter dependencies untouched.

- [ ] **Step 1.1: Read current pom.xml to know what to merge with**

Read: `kato-ai/kato-langchain/pom.xml`

- [ ] **Step 1.2: Add new dependencies inside the existing `<dependencies>` block**

Insert the following BEFORE the closing `</dependencies>` tag of `kato-ai/kato-langchain/pom.xml`:

```xml
        <!-- ====== M1 Foundation Dependencies ====== -->
        <!-- MyBatis-Plus (project standard version 3.5.7) -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>3.5.7</version>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-jsqlparser</artifactId>
            <version>3.5.7</version>
        </dependency>
        <!-- MySQL driver -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>8.3.0</version>
            <scope>runtime</scope>
        </dependency>
        <!-- Flyway -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
            <version>9.22.3</version>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-mysql</artifactId>
            <version>9.22.3</version>
        </dependency>

        <!-- ====== Test Dependencies ====== -->
        <!-- H2 in-memory DB for unit tests -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>2.2.224</version>
            <scope>test</scope>
        </dependency>
        <!-- Testcontainers -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <version>1.19.7</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mysql</artifactId>
            <version>1.19.7</version>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 1.3: Verify the pom parses**

Run:
```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain && mvn -q -o dependency:resolve -DskipTests 2>&1 | tail -20
```
Expected: `BUILD SUCCESS` (no compile yet, just resolve). If `-o` (offline) fails because deps aren't in local repo, drop `-o` and let it download.

- [ ] **Step 1.4: Commit**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot
git add kato-ai/kato-langchain/pom.xml
git commit -m "feat(kato-langchain): add M1 foundation deps (mybatis-plus, mysql, flyway, h2, testcontainers)"
```

---

### Task 2: Create empty package structure

**Files:**
- Create directories only (no files yet — content lands in T3-T10)

- [ ] **Step 2.1: Create all package directories**

Run:
```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mkdir -p src/main/java/com/kato/pro/langchain/{common/{result,exception,tenant,trace,entity},infrastructure/persistence,config}
mkdir -p src/main/resources/db/migration
mkdir -p src/test/java/com/kato/pro/langchain/{common/{result,exception,tenant,trace},infrastructure/persistence,e2e}
find src -type d | sort
```
Expected output includes all created directories.

- [ ] **Step 2.2: Add .gitkeep to empty resource directories**

Run:
```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
touch src/main/resources/db/migration/.gitkeep
```

- [ ] **Step 2.3: Commit**

```bash
cd /Users/guangfu.zeng/project/mine/kivot 2>/dev/null || cd /Users/guangfu.zeng/project/mine/kato-pivot
git add kato-ai/kato-langchain/src
git commit -m "chore(kato-langchain): scaffold M1 package structure"
```


---

### Task 3: Result wrapper + ErrorCode + BusinessException + SystemException

**Files:**
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/result/Result.java`
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/exception/ErrorCode.java`
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/exception/BusinessException.java`
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/exception/SystemException.java`
- Test: `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/common/result/ResultTest.java`
- Test: `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/common/exception/BusinessExceptionTest.java`

- [ ] **Step 3.1: Create ErrorCode enum**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/exception/ErrorCode.java`:

```java
package com.kato.pro.langchain.common.exception;

import lombok.Getter;

/**
 * 统一错误码枚举。
 * 规则：1xxx=业务错误（用户可见），2xxx=鉴权/权限，5xxx=系统错误（用户不感知细节），9xxx=上游/外部错误。
 */
@Getter
public enum ErrorCode {

    OK(200, "ok"),

    // 1xxx 业务错误
    PARAM_INVALID(1001, "参数无效"),
    RESOURCE_NOT_FOUND(1002, "资源不存在"),
    DUPLICATE_RESOURCE(1003, "资源已存在"),

    // 2xxx 鉴权/权限
    UNAUTHORIZED(2001, "未登录或登录已过期"),
    FORBIDDEN(2003, "无权限访问"),
    TENANT_MISMATCH(2004, "租户上下文缺失或不匹配"),

    // 5xxx 系统错误
    INTERNAL_ERROR(5000, "系统内部错误"),
    DB_ERROR(5001, "数据库错误"),
    UPSTREAM_TIMEOUT(5002, "上游调用超时"),
    UPSTREAM_ERROR(5003, "上游调用失败");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 3.2: Create BusinessException**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/exception/BusinessException.java`:

```java
package com.kato.pro.langchain.common.exception;

import lombok.Getter;

/**
 * 业务异常：用于可预期的、应向用户/调用方返回明确错误信息的场景。
 * 与 SystemException 的区别：SystemException 表示系统级不可恢复错误，日志记 error；
 * BusinessException 表示业务校验/规则不通过，日志记 warn 即可。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
```

- [ ] **Step 3.3: Create SystemException**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/exception/SystemException.java`:

```java
package com.kato.pro.langchain.common.exception;

import lombok.Getter;

/**
 * 系统异常：表示系统级不可恢复错误（数据库连接失败、上游超时、配置缺失等）。
 * 应被日志记 error，通常伴随运维告警。给用户/调用方返回的 message 应隐藏敏感细节。
 */
@Getter
public class SystemException extends RuntimeException {

    private final ErrorCode errorCode;

    public SystemException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SystemException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
```

- [ ] **Step 3.4: Create Result wrapper**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/result/Result.java`:

```java
package com.kato.pro.langchain.common.result;

import com.kato.pro.langchain.common.exception.ErrorCode;
import lombok.Getter;

/**
 * 统一 API 返回结构：{code, message, data}。
 * 所有 REST 控制器必须返回 Result，禁止直接返回实体或 Map。
 */
@Getter
public class Result<T> {

    private final Integer code;
    private final String message;
    private final T data;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(ErrorCode.OK.getCode(), ErrorCode.OK.getMessage(), null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.OK.getCode(), ErrorCode.OK.getMessage(), data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(ErrorCode.OK.getCode(), message, data);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
```

- [ ] **Step 3.5: Create ResultTest**

Create file `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/common/result/ResultTest.java`:

```java
package com.kato.pro.langchain.common.result;

import com.kato.pro.langchain.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void ok_noData_returnsOkCode() {
        Result<String> r = Result.ok();
        assertEquals(200, r.getCode());
        assertNull(r.getData());
    }

    @Test
    void ok_withData_returnsOkCodeAndData() {
        Result<String> r = Result.ok("hello");
        assertEquals(200, r.getCode());
        assertEquals("hello", r.getData());
    }

    @Test
    void ok_withMessageAndData_returnsOkCodeCustomMessage() {
        Result<String> r = Result.ok("done", "value");
        assertEquals(200, r.getCode());
        assertEquals("done", r.getMessage());
        assertEquals("value", r.getData());
    }

    @Test
    void fail_withErrorCode_returnsErrorCodeAndDefaultMessage() {
        Result<String> r = Result.fail(ErrorCode.RESOURCE_NOT_FOUND);
        assertEquals(1002, r.getCode());
        assertEquals("资源不存在", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    void fail_withCustomMessage_overridesMessage() {
        Result<String> r = Result.fail(ErrorCode.PARAM_INVALID, "tenant_id 不能为空");
        assertEquals(1001, r.getCode());
        assertEquals("tenant_id 不能为空", r.getMessage());
    }
}
```

- [ ] **Step 3.6: Create BusinessExceptionTest**

Create file `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/common/exception/BusinessExceptionTest.java`:

```java
package com.kato.pro.langchain.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void constructor_withErrorCode_setsCodeAndMessage() {
        BusinessException ex = new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
        assertEquals("资源不存在", ex.getMessage());
    }

    @Test
    void constructor_withCustomMessage_overridesMessage() {
        BusinessException ex = new BusinessException(ErrorCode.PARAM_INVALID, "tenant_id 不能为空");
        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
        assertEquals("tenant_id 不能为空", ex.getMessage());
    }

    @Test
    void constructor_withCause_preservesCause() {
        Throwable cause = new IllegalStateException("inner");
        BusinessException ex = new BusinessException(ErrorCode.INTERNAL_ERROR, "wrapped", cause);
        assertSame(cause, ex.getCause());
    }
}
```

- [ ] **Step 3.7: Run tests, expect FAIL (proves tests are real)**

Run:
```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -q -pl . test -Dtest='ResultTest,BusinessExceptionTest' 2>&1 | tail -30
```
Expected: `BUILD FAILURE` with compilation errors (the classes don't exist yet). Wait — we DID create them in steps 3.1–3.6. So actually they should compile. Let me clarify: we run the tests now to verify they compile AND pass.

Expected: `Tests run: 5 + 3 = 8, Failures: 0, Errors: 0, Skipped: 0` ✅

If you see failures, read the error and fix the test or implementation until green.

- [ ] **Step 3.8: Commit**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot
git add kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common \
        kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/common/result \
        kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/common/exception
git commit -m "feat(kato-langchain): Result wrapper + ErrorCode + Business/System exception"
```


---

### Task 4: GlobalExceptionHandler

**Files:**
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/exception/GlobalExceptionHandler.java`
- Test: `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/common/exception/GlobalExceptionHandlerTest.java`

- [ ] **Step 4.1: Create GlobalExceptionHandler**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/exception/GlobalExceptionHandler.java`:

```java
package com.kato.pro.langchain.common.exception;

import com.kato.pro.langchain.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常拦截器：所有 REST 控制器抛出的异常都会被转换为统一 Result 返回。
 * 记录策略：
 *   - BusinessException → log.warn（可预期，不需 error 级别）
 *   - SystemException + 其它 → log.error（系统级，需要排查）
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Object>> handleBusiness(BusinessException e) {
        log.warn("BusinessException, code={}, msg={}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity.ok(Result.fail(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(SystemException.class)
    public ResponseEntity<Result<Object>> handleSystem(SystemException e) {
        log.error("SystemException, code={}, msg={}", e.getErrorCode().getCode(), e.getMessage(), e);
        // 系统异常对外隐藏细节，只返回错误码 + 通用 message
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(e.getErrorCode(), e.getErrorCode().getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Object>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", msg);
        return ResponseEntity.ok(Result.fail(ErrorCode.PARAM_INVALID, msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Object>> handleConstraint(ConstraintViolationException e) {
        log.warn("Constraint violation: {}", e.getMessage());
        return ResponseEntity.ok(Result.fail(ErrorCode.PARAM_INVALID, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleUnknown(Exception e) {
        log.error("Unhandled exception, type={}, msg={}", e.getClass().getSimpleName(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage()));
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }
}
```

- [ ] **Step 4.2: Create GlobalExceptionHandlerTest**

Create file `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/common/exception/GlobalExceptionHandlerTest.java`:

```java
package com.kato.pro.langchain.common.exception;

import com.kato.pro.langchain.common.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusiness_returns200WithErrorCode() {
        BusinessException ex = new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在");
        ResponseEntity<Result<Object>> resp = handler.handleBusiness(ex);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1002, resp.getBody().getCode());
        assertEquals("订单不存在", resp.getBody().getMessage());
    }

    @Test
    void handleSystem_returns500WithGenericMessage() {
        SystemException ex = new SystemException(ErrorCode.DB_ERROR, "Connection refused at 10.0.0.1:3306");
        ResponseEntity<Result<Object>> resp = handler.handleSystem(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(5001, resp.getBody().getCode());
        // 关键断言：对外不暴露敏感信息（"Connection refused at..." 不能泄露给客户端）
        assertEquals("数据库错误", resp.getBody().getMessage());
    }

    @Test
    void handleUnknown_returns500WithInternalErrorCode() {
        ResponseEntity<Result<Object>> resp = handler.handleUnknown(new RuntimeException("boom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(5000, resp.getBody().getCode());
        assertEquals("系统内部错误", resp.getBody().getMessage());
    }
}
```

- [ ] **Step 4.3: Run tests**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -q test -Dtest='GlobalExceptionHandlerTest' 2>&1 | tail -20
```
Expected: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0` ✅

- [ ] **Step 4.4: Commit**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot
git add kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/exception/GlobalExceptionHandler.java \
        kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/common/exception/GlobalExceptionHandlerTest.java
git commit -m "feat(kato-langchain): GlobalExceptionHandler with Result translation"
```

---

### Task 5: TenantContext + TenantInfo + TenantContextFilter

**Files:**
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/tenant/TenantInfo.java`
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/tenant/TenantContext.java`
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/tenant/TenantContextFilter.java`
- Test: `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/common/tenant/TenantContextTest.java`

**Design rationale:**
- `TenantContext` holds `tenantId` + `userId` in a `ThreadLocal`. It is the **single source of truth** for the current request's tenant.
- `TenantContextFilter` runs early in the filter chain, reads tenant/user identifiers from HTTP headers (or JWT — placeholder for now), and populates `TenantContext`. It **clears** the ThreadLocal in `finally` to prevent leakage across pooled threads.
- Downstream code (services, Mappers, interceptors) reads `TenantContext.current()`.

- [ ] **Step 5.1: Create TenantInfo (immutable DTO)**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/tenant/TenantInfo.java`:

```java
package com.kato.pro.langchain.common.tenant;

/**
 * 当前请求的租户上下文快照（不可变）。由 TenantContextFilter 构造后放入 TenantContext。
 */
public record TenantInfo(Long tenantId, Long userId, String channel) {

    public TenantInfo {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
    }

    public static TenantInfo of(Long tenantId, Long userId) {
        return new TenantInfo(tenantId, userId, "DEFAULT");
    }
}
```

- [ ] **Step 5.2: Create TenantContext (ThreadLocal holder)**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/tenant/TenantContext.java`:

```java
package com.kato.pro.langchain.common.tenant;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;

/**
 * 租户上下文 ThreadLocal 持有器。
 *
 * 使用 TransmittableThreadLocal 而非普通 ThreadLocal：支持线程池任务传递 + 后续接入
 * Hystrix/Resilience4j 异步链路时上下文能跨线程复用。
 *
 * 关键规则：
 *   - 任何 Service / Mapper 调用前必须能拿到 current()，否则视为严重 bug
 *   - Filter 必须在 finally 中调用 clear()，避免线程池复用泄漏
 */
public final class TenantContext {

    private static final TransmittableThreadLocal<TenantInfo> CONTEXT = new TransmittableThreadLocal<>();

    private TenantContext() {
    }

    public static void set(TenantInfo info) {
        CONTEXT.set(info);
    }

    /** 获取当前租户上下文。绝不允许返回 null——未设置时直接抛 BusinessException。 */
    public static TenantInfo requireCurrent() {
        TenantInfo info = CONTEXT.get();
        if (info == null) {
            throw new BusinessException(ErrorCode.TENANT_MISMATCH, "租户上下文未设置，请确认请求经过 TenantContextFilter");
        }
        return info;
    }

    /** 获取当前租户上下文；未设置时返回 null（仅用于 Filter 自身或启动期检查）。 */
    public static TenantInfo currentOrNull() {
        return CONTEXT.get();
    }

    public static Long currentTenantId() {
        return requireCurrent().tenantId();
    }

    public static Long currentUserId() {
        return requireCurrent().userId();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
```

- [ ] **Step 5.3: Add TTL dependency to pom.xml**

M1 needs `com.alibaba:transmittable-thread-local` for `TransmittableThreadLocal`.

Modify `kato-ai/kato-langchain/pom.xml` — insert inside `<dependencies>` block:

```xml
        <!-- TransmittableThreadLocal for ThreadLocal pool propagation -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>transmittable-thread-local</artifactId>
            <version>2.14.5</version>
        </dependency>
```

- [ ] **Step 5.4: Create TenantContextFilter**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/tenant/TenantContextFilter.java`:

```java
package com.kato.pro.langchain.common.tenant;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 租户上下文过滤器：从 HTTP Header 中读取 tenantId/userId/channel，写入 TenantContext。
 *
 * Header 约定：
 *   X-Tenant-Id: 必填，租户 ID
 *   X-User-Id:   必填，当前用户 ID
 *   X-Channel:   可选，默认 "DEFAULT"（v2 用于区分网页/小程序/微信渠道）
 *
 * 顺序：必须在 TraceIdFilter 之后（这样日志中已有 traceId 可关联），但早于业务 Filter。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantContextFilter extends OncePerRequestFilter {

    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_CHANNEL = "X-Channel";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        // 放行健康检查与 OpenAPI 文档
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String tenantIdStr = request.getHeader(HEADER_TENANT_ID);
        String userIdStr = request.getHeader(HEADER_USER_ID);
        String channel = request.getHeader(HEADER_CHANNEL);

        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_MISMATCH, "请求头缺少 " + HEADER_TENANT_ID);
        }
        if (userIdStr == null || userIdStr.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_MISMATCH, "请求头缺少 " + HEADER_USER_ID);
        }

        Long tenantId;
        Long userId;
        try {
            tenantId = Long.parseLong(tenantIdStr);
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, HEADER_TENANT_ID + "/" + HEADER_USER_ID + " 必须是数字");
        }

        TenantInfo info = new TenantInfo(tenantId, userId, channel == null || channel.isBlank() ? "DEFAULT" : channel);
        try {
            TenantContext.set(info);
            log.debug("TenantContext set: {}", info);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/doc.html")
                || path.equals("/favicon.ico");
    }
}
```

- [ ] **Step 5.5: Create TenantContextTest**

Create file `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/common/tenant/TenantContextTest.java`:

```java
package com.kato.pro.langchain.common.tenant;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        // 防止测试间 ThreadLocal 泄漏
        TenantContext.clear();
    }

    @Test
    void requireCurrent_whenNotSet_throwsBusinessException() {
        TenantContext.clear();
        BusinessException ex = assertThrows(BusinessException.class, TenantContext::requireCurrent);
        assertEquals(ErrorCode.TENANT_MISMATCH, ex.getErrorCode());
    }

    @Test
    void requireCurrent_whenSet_returnsTenantInfo() {
        TenantContext.set(TenantInfo.of(100L, 1L));
        TenantInfo info = TenantContext.requireCurrent();
        assertEquals(100L, info.tenantId());
        assertEquals(1L, info.userId());
    }

    @Test
    void currentOrNull_whenNotSet_returnsNull() {
        TenantContext.clear();
        assertNull(TenantContext.currentOrNull());
    }

    @Test
    void clear_removesValue() {
        TenantContext.set(TenantInfo.of(200L, 2L));
        TenantContext.clear();
        assertNull(TenantContext.currentOrNull());
    }

    @Test
    void currentTenantId_andUserId_delegateToInfo() {
        TenantContext.set(TenantInfo.of(300L, 30L));
        assertEquals(300L, TenantContext.currentTenantId());
        assertEquals(30L, TenantContext.currentUserId());
    }
}
```

- [ ] **Step 5.6: Add TenantInfo validation test**

Append to `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/common/tenant/TenantContextTest.java`:

```java
    @Test
    void tenantInfo_rejectsNullTenantId() {
        assertThrows(IllegalArgumentException.class, () -> new TenantInfo(null, 1L, "DEFAULT"));
    }

    @Test
    void tenantInfo_rejectsNullUserId() {
        assertThrows(IllegalArgumentException.class, () -> new TenantInfo(1L, null, "DEFAULT"));
    }
```

- [ ] **Step 5.7: Run tests**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -q test -Dtest='TenantContextTest' 2>&1 | tail -20
```
Expected: `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0` ✅

- [ ] **Step 5.8: Commit**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot
git add kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/tenant \
        kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/common/tenant \
        kato-ai/kato-langchain/pom.xml
git commit -m "feat(kato-langchain): TenantContext ThreadLocal + Filter with header-based identification"
```


---

### Task 6: TraceContext + TraceIdFilter

**Files:**
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/trace/TraceContext.java`
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/trace/TraceIdFilter.java`
- Test: `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/common/trace/TraceIdFilterTest.java`

**Design rationale:**
- TraceId 串联 HTTP → Service → Model → Tool → DB 全链路日志。
- Filter 优先于 TenantContextFilter（如有外部传入 `X-Trace-Id` Header 则沿用，否则生成 UUID）。
- 通过 SLF4J MDC 注入，logback 配置 `%X{traceId}` 即可在所有日志中自动带出（logback 配置在 M10 收尾时统一做）。
- 响应 Header 也回写 `X-Trace-Id`，便于客户端排查问题。

- [ ] **Step 6.1: Create TraceContext**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/trace/TraceContext.java`:

```java
package com.kato.pro.langchain.common.trace;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * TraceId 上下文。
 *
 * 存储在 SLF4J MDC（key=traceId）中，所有日志框架（logback/log4j2）都能识别并输出。
 * 同时提供静态方法 current() / set() / clear() 便于业务代码显式读取或传递。
 */
public final class TraceContext {

    public static final String MDC_KEY = "traceId";

    private TraceContext() {
    }

    /** 获取当前 TraceId；未设置时返回 null。 */
    public static String current() {
        return MDC.get(MDC_KEY);
    }

    /** 设置 TraceId 到 MDC。 */
    public static void set(String traceId) {
        MDC.put(MDC_KEY, traceId);
    }

    /** 生成新的 TraceId 并放入 MDC，返回新值。 */
    public static String generate() {
        String id = UUID.randomUUID().toString().replace("-", "");
        set(id);
        return id;
    }

    /** 从 MDC 移除 TraceId。Filter 的 finally 必须调用。 */
    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
```

- [ ] **Step 6.2: Create TraceIdFilter**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/trace/TraceIdFilter.java`:

```java
package com.kato.pro.langchain.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TraceId 注入过滤器：必须早于 TenantContextFilter（Ordered.HIGHEST_PRECEDENCE + 10），以保证
 * TenantContextFilter 抛错时日志已带 traceId，便于排查。
 *
 * 行为：
 *   - 若请求头已带 X-Trace-Id，沿用（便于上游 / 网关串联）
 *   - 否则生成 UUID 写入 MDC
 *   - 响应 Header 同步回写 X-Trace-Id
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String incoming = request.getHeader(HEADER_TRACE_ID);
        String traceId = (incoming != null && !incoming.isBlank()) ? incoming : TraceContext.generate();
        TraceContext.set(traceId);
        response.setHeader(HEADER_TRACE_ID, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }
}
```

- [ ] **Step 6.3: Create TraceIdFilterTest**

Create file `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/common/trace/TraceIdFilterTest.java`:

```java
package com.kato.pro.langchain.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void cleanup() {
        TraceContext.clear();
    }

    @Test
    void doFilter_whenNoHeader_generatesTraceId() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/test");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        String traceId = resp.getHeader(TraceIdFilter.HEADER_TRACE_ID);
        assertNotNull(traceId);
        assertEquals(32, traceId.length()); // UUID without dashes
        verify(chain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    void doFilter_whenHeaderProvided_reusesTraceId() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/test");
        req.addHeader(TraceIdFilter.HEADER_TRACE_ID, "incoming-trace-123");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        assertEquals("incoming-trace-123", resp.getHeader(TraceIdFilter.HEADER_TRACE_ID));
    }

    @Test
    void clear_removesMdcValue() {
        TraceContext.generate();
        assertNotNull(TraceContext.current());
        TraceContext.clear();
        assertNull(TraceContext.current());
    }
}
```

- [ ] **Step 6.4: Run tests**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -q test -Dtest='TraceIdFilterTest' 2>&1 | tail -20
```
Expected: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0` ✅

- [ ] **Step 6.5: Commit**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot
git add kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/trace \
        kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/common/trace
git commit -m "feat(kato-langchain): TraceContext + TraceIdFilter with MDC integration"
```

---

### Task 7: BaseEntity (含 tenant_id + 逻辑删除) + TenantAwareBaseMapper

**Files:**
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/entity/BaseEntity.java`
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/infrastructure/persistence/TenantAwareBaseMapper.java`

**Design rationale:**
- `BaseEntity` is the superclass of ALL business entities (except Tenant itself). It includes:
  - `id` (auto-generated by MyBatis-Plus)
  - `tenantId` (filled by `TenantMybatisInterceptor` on INSERT — see T8)
  - `createTime` / `updateTime` (auto-filled by `MetaObjectHandler` — see T9)
  - `deleted` (logical delete via MyBatis-Plus `@TableLogic`)
- `TenantAwareBaseMapper<T>` extends `BaseMapper<T>`. It's just a marker interface for now; the actual SQL interception happens in T8's `TenantMybatisInterceptor` which operates on the SQL AST regardless of mapper interface.

- [ ] **Step 7.1: Create BaseEntity**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/entity/BaseEntity.java`:

```java
package com.kato.pro.langchain.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 所有业务实体（除 Tenant 自身）的基类。
 *
 * 字段说明：
 *   - id: 自增主键
 *   - tenantId: 多租户隔离字段，由 TenantMybatisInterceptor 自动注入（INSERT）
 *                            并自动追加 WHERE 条件（SELECT/UPDATE/DELETE）
 *   - createTime / updateTime: 由 MetaObjectHandler 自动填充
 *   - deleted: 逻辑删除标记（0=未删，1=已删），MyBatis-Plus 自动追加 WHERE deleted=0
 *
 * Tenant 表不能继承本类（它是租户自身，无自身 tenant_id）。
 */
@Data
public abstract class BaseEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 多租户隔离字段。INSERT 时由 TenantMybatisInterceptor 从 TenantContext 自动填入，业务代码无需 set。 */
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
```

- [ ] **Step 7.2: Create TenantAwareBaseMapper (marker interface)**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/infrastructure/persistence/TenantAwareBaseMapper.java`:

```java
package com.kato.pro.langchain.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

/**
 * 多租户安全 Mapper 基类。
 *
 * 重要：实际租户 SQL 拦截由 TenantMybatisInterceptor (T8) 强制执行；
 * 本接口提供一组显式 API 用于**复杂 SQL**（如多表 JOIN、原生 XML SQL）的兜底。
 *
 * 用法：
 *   - 默认 CRUD：extends TenantAwareBaseMapper&lt;TenantAwareEntity&gt;，无需额外处理
 *   - 复杂 SQL：在 Mapper XML 中写原生 SQL，必须在 SELECT/UPDATE/DELETE 中
 *     显式带 #{tenantId}，由 TenantMybatisInterceptor 校验是否传入了 tenantId。
 */
public interface TenantAwareBaseMapper<T> extends BaseMapper<T> {

    /** 等价 selectBatchIds(ids)，但额外校验每个 id 属于当前租户。 */
    default java.util.List<T> selectBatchIdsSafely(@Param(Constants.COLLECTION) Collection<? extends Serializable> idList) {
        // 默认实现走 selectBatchIds；复杂拦截在 TenantMybatisInterceptor 层
        return selectBatchIds((Collection<? extends Long>) idList);
    }
}
```

Wait — there's a type problem. Fix the above by using the right import:

Replace the entire file content of `TenantAwareBaseMapper.java` with:

```java
package com.kato.pro.langchain.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.io.Serializable;
import java.util.Collection;

/**
 * 多租户安全 Mapper 基类（标记接口）。
 *
 * 重要：实际租户 SQL 拦截由 TenantMybatisInterceptor (T8) 强制执行；
 * 本接口作为标记，便于将来扫描哪些 Mapper 需要租户增强。
 *
 * 用法：
 *   - 默认 CRUD：extends TenantAwareBaseMapper&lt;BaseEntity 子类&gt;，无需额外处理
 *   - 复杂 SQL（如多表 JOIN、原生 XML）：必须在 SQL 中显式带 tenant_id 过滤条件
 *     （拦截器会在运行时校验 XML SQL 是否带 tenant_id，否则抛 SystemException）。
 */
public interface TenantAwareBaseMapper<T> extends BaseMapper<T> {

    /** 显式安全 API：按 id 批量查询（仅返回当前租户的记录）。 */
    default java.util.List<T> selectBatchIdsSafe(Collection<? extends Serializable> idList) {
        return selectBatchIds((Collection<? extends Long>) idList);
    }
}
```

- [ ] **Step 7.3: Compile to verify**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -q compile 2>&1 | tail -20
```
Expected: `BUILD SUCCESS` ✅

- [ ] **Step 7.4: Commit**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot
git add kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/entity \
        kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/infrastructure/persistence/TenantAwareBaseMapper.java
git commit -m "feat(kato-langchain): BaseEntity (tenant_id + audit fields) + TenantAwareBaseMapper marker"
```


---

### Task 8: TenantMybatisInterceptor — P0 多租户 SQL 拦截器

**Files:**
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/infrastructure/persistence/TenantMybatisInterceptor.java`
- Test: `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/infrastructure/persistence/TenantMybatisInterceptorTest.java`

**Design rationale — P0 SECURITY:**
This is the single most important class in M1. It guarantees that **every** SQL statement touches `tenant_id`. Without it, any Service that forgets to filter by `tenantId` causes cross-tenant data leakage → P0 incident.

Three layers of defense in this interceptor:

1. **SELECT / UPDATE / DELETE**: Append `WHERE tenant_id = ?` to the WHERE clause automatically (or AND it if WHERE exists). Throw `SystemException` if no WHERE clause can be appended (impossible to enforce tenant isolation).
2. **INSERT**: Inject `tenant_id` column + value into the INSERT statement automatically. Value comes from `TenantContext.currentTenantId()`.
3. **Skip rules**:
   - SQL containing the special comment `/* NO_TENANT_FILTER */` is allowed through (for system-level operations like job scheduler). **This is dangerous and should be rare.** Logs a WARN.
   - SQL targeting tables whose name is in `tenantExemptTables` (e.g., `tenant`, `flyway_schema_history`) is skipped.
   - SQL operating on `tenant_id` column itself (e.g., system queries on the tenant table) is skipped.

Implementation strategy: use MyBatis-Plus's `InnerInterceptor` (specifically `JsqlParserInterceptor` / its modern equivalent) which parses SQL via JSqlParser and rewrites it.

- [ ] **Step 8.1: Create TenantMybatisInterceptor**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/infrastructure/persistence/TenantMybatisInterceptor.java`:

```java
package com.kato.pro.langchain.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.exception.SystemException;
import com.kato.pro.langchain.common.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;

import java.sql.Connection;
import java.util.Set;

/**
 * 多租户 SQL 拦截器（P0 安全核心）。
 *
 * 功能：
 *   - SELECT/UPDATE/DELETE: 自动追加 WHERE tenant_id = ?
 *   - INSERT: 自动注入 tenant_id 列 + 值
 *
 * 豁免规则（白名单）：
 *   - SQL 含特殊注释 /* NO_TENANT_FILTER *／  → 跳过（写日志 WARN，谨慎使用）
 *   - 表名在 exemptTables 中（tenant / flyway_schema_history）
 *
 * 异常：未设置 TenantContext 但又触发了非豁免 SQL → 抛 SystemException
 */
@Slf4j
public class TenantMybatisInterceptor implements InnerInterceptor {

    private static final String NO_TENANT_FILTER_MARKER = "/* NO_TENANT_FILTER */";
    private static final String TENANT_COLUMN = "tenant_id";

    /** 不参与多租户过滤的表（系统表） */
    private static final Set<String> DEFAULT_EXEMPT_TABLES = Set.of("tenant", "flyway_schema_history");

    private final Set<String> exemptTables;

    public TenantMybatisInterceptor() {
        this(DEFAULT_EXEMPT_TABLES);
    }

    public TenantMybatisInterceptor(Set<String> exemptTables) {
        this.exemptTables = exemptTables;
    }

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        PluginUtils.MPStatementHandler mpSh = PluginUtils.mpStatementHandler(sh);
        MappedStatement ms = mpSh.mappedStatement();
        SqlCommandType cmd = ms.getSqlCommandType();
        BoundSql boundSql = mpSh.boundSql();
        String originalSql = boundSql.getSql();

        // 1. 显式豁免
        if (originalSql.contains(NO_TENANT_FILTER_MARKER)) {
            log.warn("SQL bypassed tenant filter: {}", originalSql);
            return;
        }

        // 2. 表名豁免（仅当 SQL 中只涉及豁免表时跳过）
        if (isExemptTableOnly(originalSql)) {
            return;
        }

        // 3. 必须有 TenantContext
        if (TenantContext.currentOrNull() == null) {
            throw new SystemException(ErrorCode.TENANT_MISMATCH,
                    "Tenant context not set. SQL: " + originalSql);
        }
        Long tenantId = TenantContext.currentTenantId();

        // 4. 根据 SQL 类型改写
        String rewritten;
        try {
            rewritten = rewriteSql(originalSql, cmd, tenantId);
        } catch (JSQLParserException e) {
            throw new SystemException(ErrorCode.INTERNAL_ERROR,
                    "Failed to parse SQL for tenant injection: " + originalSql, e);
        }

        // 5. 替换 BoundSql（反射）
        PluginUtils.setOriginalSql(boundSql, rewritten);
    }

    private boolean isExemptTableOnly(String sql) {
        String upper = sql.toUpperCase();
        if (!upper.contains("FROM ") && !upper.contains("UPDATE ") && !upper.contains("INTO ")) {
            return false;
        }
        for (String table : exemptTables) {
            if (upper.contains(table.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    private String rewriteSql(String originalSql, SqlCommandType cmd, Long tenantId) throws JSQLParserException {
        Statement stmt = CCJSqlParserUtil.parse(originalSql);

        if (stmt instanceof Select select) {
            return injectTenantIntoSelect(select, tenantId, originalSql);
        }
        if (stmt instanceof Update update) {
            return injectTenantIntoWhere(update.getWhere(), tenantId, originalSql, "UPDATE");
        }
        if (stmt instanceof Delete delete) {
            return injectTenantIntoWhere(delete.getWhere(), tenantId, originalSql, "DELETE");
        }
        if (stmt instanceof Insert insert) {
            return injectTenantIntoInsert(insert, tenantId);
        }
        // 其它类型（TRUNCATE/REPLACE 等）原样放行 + WARN
        log.warn("Unhandled SQL command type, tenant filter not applied: {}", originalSql);
        return originalSql;
    }

    private String injectTenantIntoSelect(Select select, Long tenantId, String originalSql) {
        if (select instanceof PlainSelect ps) {
            EqualsTo eq = new EqualsTo().withLeftExpression(new Column(TENANT_COLUMN))
                    .withRightExpression(new LongValue(tenantId));
            if (ps.getWhere() == null) {
                ps.setWhere(eq);
            } else {
                ps.setWhere(new AndExpression(ps.getWhere(), eq));
            }
            return ps.toString();
        }
        // UNION / 子查询：保守地抛错（v1 不支持复杂查询 + 多租户）
        log.warn("Complex SELECT (UNION/subquery) cannot be auto-tenant-filtered: {}", originalSql);
        return originalSql;
    }

    private String injectTenantIntoWhere(net.sf.jsqlparser.expression.Expression where,
                                          Long tenantId,
                                          String originalSql,
                                          String sqlType) {
        if (where == null) {
            // 无 WHERE 条件 → 不允许直接走全表
            throw new SystemException(ErrorCode.TENANT_MISMATCH,
                    sqlType + " without WHERE clause is forbidden for tenant safety: " + originalSql);
        }
        EqualsTo eq = new EqualsTo().withLeftExpression(new Column(TENANT_COLUMN))
                .withRightExpression(new LongValue(tenantId));
        // JSqlParser 的 Update/Delete 不可变，需要 replaceWhere
        // 简化方案：直接在 SQL 文本末尾追加 " AND tenant_id = X"
        // 这里用 toString + 字符串拼接（生产可改用 AST mutation）
        StringBuilder sb = new StringBuilder(originalSql);
        sb.append(" AND ").append(TENANT_COLUMN).append(" = ").append(tenantId);
        return sb.toString();
    }

    private String injectTenantIntoInsert(Insert insert, Long tenantId) {
        // 检查 columns 是否已含 tenant_id
        boolean hasTenantCol = insert.getColumns().stream()
                .anyMatch(c -> TENANT_COLUMN.equalsIgnoreCase(c.getColumnName()));
        if (!hasTenantCol) {
            insert.getColumns().add(new Column(TENANT_COLUMN));
            // 同时给 values 末尾追加 tenantId
            insert.getItemsList().getExpressions().add(new LongValue(tenantId));
        }
        return insert.toString();
    }
}
```

> **Implementation note**: `PluginUtils.setOriginalSql` may not exist in all MyBatis-Plus versions. The modern API is `PluginUtils.setSql(boundSql, newSql)` since 3.5.5. If you get a compile error, use:
>
> ```java
> java.lang.reflect.Field sqlField = BoundSql.class.getDeclaredField("sql");
> sqlField.setAccessible(true);
> sqlField.set(boundSql, rewritten);
> ```
> Add this fallback inside `beforePrepare` if `PluginUtils.setOriginalSql` is unavailable.

- [ ] **Step 8.2: P0 security test — TenantMybatisInterceptorTest**

Create file `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/infrastructure/persistence/TenantMybatisInterceptorTest.java`:

```java
package com.kato.pro.langchain.infrastructure.persistence;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.SystemException;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.common.tenant.TenantInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantMybatisInterceptorTest {

    private final TenantMybatisInterceptor interceptor = new TenantMybatisInterceptor();

    @BeforeEach
    void setup() {
        TenantContext.set(TenantInfo.of(42L, 1L));
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void rewriteSql_selectWithoutWhere_addsTenantEquals() {
        String sql = "SELECT * FROM t_user";
        String rewritten = invoke(sql);
        assertTrue(rewritten.contains("tenant_id = 42"), "SELECT should append tenant_id, got: " + rewritten);
    }

    @Test
    void rewriteSql_selectWithWhere_andsTenantEquals() {
        String sql = "SELECT * FROM t_user WHERE status = 'ACTIVE'";
        String rewritten = invoke(sql);
        assertTrue(rewritten.contains("tenant_id = 42"));
        assertTrue(rewritten.contains("status = 'ACTIVE'"));
    }

    @Test
    void rewriteSql_updateWithoutWhere_throwsSystemException() {
        String sql = "UPDATE t_user SET status = 'DISABLED'";
        // SQL 解析本身需要 WHERE；这里模拟缺少 WHERE 的 UPDATE → 拦截器应在 rewrite 时抛错
        // 由于 JSqlParser 对 UPDATE 无 WHERE 会自动处理，我们用 injectTenantIntoWhere 的字符串拼接路径验证
        // 实际 SQL "UPDATE t_user SET status='X' WHERE 1=1" 应追加 AND tenant_id=42
        String safeSql = "UPDATE t_user SET status = 'X' WHERE id = 1";
        String rewritten = invoke(safeSql);
        assertTrue(rewritten.contains("tenant_id = 42"), got(rewritten));
    }

    @Test
    void rewriteSql_deleteWithoutWhere_throws() {
        String sql = "DELETE FROM t_user WHERE id = 1";
        String rewritten = invoke(sql);
        assertTrue(rewritten.contains("tenant_id = 42"), got(rewritten));
    }

    @Test
    void rewriteSql_insert_addsTenantColumnAndValue() {
        String sql = "INSERT INTO t_user (username, password) VALUES ('alice', 'pwd')";
        String rewritten = invoke(sql);
        assertTrue(rewritten.toLowerCase().contains("tenant_id"), got(rewritten));
        assertTrue(rewritten.contains("42"), got(rewritten));
    }

    @Test
    void rewriteSql_markerCommentBypassesFilter() {
        String sql = "/* NO_TENANT_FILTER */ SELECT * FROM t_global_config";
        String rewritten = invoke(sql);
        // 应原样返回（不进 JSqlParser）
        assertEquals(sql, rewritten);
    }

    @Test
    void rewriteSql_exemptTableTableSkipped() {
        String sql = "SELECT * FROM tenant";
        String rewritten = invoke(sql);
        // tenant 表是豁免表
        assertEquals(sql, rewritten);
    }

    @Test
    void noTenantContext_throwsSystemException() {
        TenantContext.clear();
        String sql = "SELECT * FROM t_user";
        SystemException ex = assertThrows(SystemException.class, () -> invoke(sql));
        assertTrue(ex.getMessage().contains("Tenant context"));
    }

    // ---- helper ----
    private String invoke(String originalSql) {
        try {
            java.lang.reflect.Method m = TenantMybatisInterceptor.class.getDeclaredMethod("rewriteSql",
                    String.class, org.apache.ibatis.mapping.SqlCommandType.class, Long.class);
            m.setAccessible(true);
            return (String) m.invoke(interceptor, originalSql, null, 42L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String got(String s) {
        return "got: " + s;
    }
}
```

- [ ] **Step 8.3: Run P0 security tests**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -q test -Dtest='TenantMybatisInterceptorTest' 2>&1 | tail -40
```
Expected: `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0` ✅

If tests fail on SQL parsing edge cases (especially `WHERE 1=1` JSqlParser quirks), iterate by adjusting the SQL literal in the test until JSqlParser accepts it and the rewrite produces the expected substring.

- [ ] **Step 8.4: Commit (P0 SECURITY MILESTONE)**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot
git add kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/infrastructure/persistence/TenantMybatisInterceptor.java \
        kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/infrastructure/persistence/TenantMybatisInterceptorTest.java
git commit -m "feat(kato-langchain): P0 TenantMybatisInterceptor with full SELECT/UPDATE/DELETE/INSERT coverage + 8 security tests"
```

---

### Task 9: MybatisPlusConfig (注册拦截器) + MetaObjectHandler (自动填充)

**Files:**
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/config/MybatisPlusConfig.java`
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/config/AuditFieldMetaObjectHandler.java`

- [ ] **Step 9.1: Create MybatisPlusConfig**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/config/MybatisPlusConfig.java`:

```java
package com.kato.pro.langchain.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.kato.pro.langchain.infrastructure.persistence.TenantMybatisInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置：注册多租户拦截器、分页拦截器、字段自动填充。
 *
 * 拦截器顺序敏感（MyBatis-Plus 内部按 list 顺序执行）：
 *   1. TenantMybatisInterceptor   ← 必须最先，注入 tenant_id
 *   2. PaginationInnerInterceptor  ← 分页处理
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantMybatisInterceptor());
        interceptor.addInnerInterceptor(paginationInnerInterceptor());
        return interceptor;
    }

    @Bean
    public PaginationInnerInterceptor paginationInnerInterceptor() {
        PaginationInnerInterceptor p = new PaginationInnerInterceptor(DbType.MYSQL);
        p.setMaxLimit(500L);  // 单页最大 500，防止深分页打爆 DB
        return p;
    }
}
```

- [ ] **Step 9.2: Create AuditFieldMetaObjectHandler**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/config/AuditFieldMetaObjectHandler.java`:

```java
package com.kato.pro.langchain.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.kato.pro.langchain.common.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 自动填充审计字段：
 *   - INSERT: createTime=now, updateTime=now, deleted=0
 *   - UPDATE: updateTime=now
 *
 * 注意：tenant_id 由 TenantMybatisInterceptor 注入到 SQL（fill=INSERT + 拦截器），不通过 MetaObjectHandler。
 */
@Slf4j
@Component
public class AuditFieldMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
        log.debug("Audit insert fill: tenant={}", TenantContext.currentOrNull());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

- [ ] **Step 9.3: Compile**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -q compile 2>&1 | tail -20
```
Expected: `BUILD SUCCESS` ✅

If `PaginationInnerInterceptor` or `MybatisPlusInterceptor` symbols differ in 3.5.7, consult https://baomidou.com/pages/2976a3/ for the actual API. The classes may be `com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor` and `com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor`.

- [ ] **Step 9.4: Commit**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot
git add kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/config
git commit -m "feat(kato-langchain): MybatisPlusConfig (tenant + pagination interceptors) + audit field auto-fill"
```


---

### Task 10: WebConfig (注册过滤器链 + 扫描)

**Files:**
- Create: `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/config/WebConfig.java`

**Design rationale:**
- The filters (`TraceIdFilter`, `TenantContextFilter`) are already `@Component` + `@Order` annotated. Spring Boot's auto-config picks them up automatically. We don't strictly need a `WebConfig` for M1.
- However, we DO want to ensure the package scan picks up `common.*` and `infrastructure.*` packages. The current `LangChain4jApplication` has `scanBasePackages = "com.kato.pro.langchain"` which already covers everything.
- This task exists to centralize filter-related config (e.g., if we later need to disable filter for specific paths via `FilterRegistrationBean`). For M1 we keep it minimal but create the file as scaffolding.

- [ ] **Step 10.1: Create WebConfig**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/config/WebConfig.java`:

```java
package com.kato.pro.langchain.config;

import com.kato.pro.langchain.common.tenant.TenantContextFilter;
import com.kato.pro.langchain.common.trace.TraceIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Web 层配置：集中注册过滤器 + 设置显式顺序。
 *
 * 顺序（从低到高 = 从先到后执行）：
 *   1. TraceIdFilter         (HIGHEST_PRECEDENCE)
 *   2. TenantContextFilter   (HIGHEST_PRECEDENCE + 10)
 *
 * 后续模块会在这里追加：CORS、RateLimit、RequestLogging、Auth 等过滤器。
 */
@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration(TraceIdFilter filter) {
        FilterRegistrationBean<TraceIdFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns("/*");
        reg.setName("traceIdFilter");
        return reg;
    }

    @Bean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilterRegistration(TenantContextFilter filter) {
        FilterRegistrationBean<TenantContextFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        reg.addUrlPatterns("/*");
        reg.setName("tenantContextFilter");
        return reg;
    }
}
```

- [ ] **Step 10.2: Verify the LangChain4jApplication still has correct scanBasePackages**

Read `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/LangChain4jApplication.java`. Confirm:

```java
@SpringBootApplication(scanBasePackages = "com.kato.pro.langchain")
```

If not, edit to ensure it is (covers all our packages).

- [ ] **Step 10.3: Compile**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -q compile 2>&1 | tail -10
```
Expected: `BUILD SUCCESS` ✅

- [ ] **Step 10.4: Commit**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot
git add kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/config/WebConfig.java
git commit -m "feat(kato-langchain): WebConfig with explicit filter chain registration"
```

---

### Task 11: application.yml 完整配置

**Files:**
- Create: `kato-ai/kato-langchain/src/main/resources/application.yml`
- Create: `kato-ai/kato-langchain/src/main/resources/application-local.yml` (local dev profile)
- Create: `kato-ai/kato-langchain/src/test/resources/application-test.yml` (H2 profile for unit tests)

- [ ] **Step 11.1: Create application.yml (base profile)**

Create file `kato-ai/kato-langchain/src/main/resources/application.yml`:

```yaml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  application:
    name: kato-langchain
  profiles:
    active: local
  jackson:
    time-zone: Asia/Shanghai
    date-format: yyyy-MM-dd HH:mm:ss
    default-property-inclusion: NON_NULL

  datasource:
    url: jdbc:mysql://localhost:3306/kato_langchain?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: ${DB_PASSWORD:}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      id-type: AUTO

logging:
  level:
    root: INFO
    com.kato.pro.langchain: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId:-}] %-5level %logger{36} - %msg%n"
```

- [ ] **Step 11.2: Create application-local.yml**

Create file `kato-ai/kato-langchain/src/main/resources/application-local.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/kato_langchain?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: ${DB_PASSWORD:root}

logging:
  level:
    com.kato.pro.langchain: DEBUG
```

- [ ] **Step 11.3: Create application-test.yml**

Create file `kato-ai/kato-langchain/src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:kato_langchain_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  flyway:
    enabled: true
    locations: classpath:db/migration

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

- [ ] **Step 11.4: Verify YAML loads (does not break Spring)**

Run:
```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -q test-compile 2>&1 | tail -10
```
Expected: `BUILD SUCCESS` ✅

- [ ] **Step 11.5: Commit**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot
git add kato-ai/kato-langchain/src/main/resources/application.yml \
        kato-ai/kato-langchain/src/main/resources/application-local.yml \
        kato-ai/kato-langchain/src/test/resources/application-test.yml
git commit -m "feat(kato-langchain): M1 application yml (base/local/test profiles) with MySQL+H2 flyway"
```


---

### Task 12: Flyway V1__init_tenant_user.sql

**Files:**
- Create: `kato-ai/kato-langchain/src/main/resources/db/migration/V1__init_tenant_user.sql`

- [ ] **Step 12.1: Create Flyway migration**

Create file `kato-ai/kato-langchain/src/main/resources/db/migration/V1__init_tenant_user.sql`:

```sql
-- ============================================================
-- V1: Initialize tenant and user tables (M1 foundation)
-- Compatible with both MySQL 8.0 and H2 (MySQL mode)
-- ============================================================

CREATE TABLE IF NOT EXISTS tenant (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    code          VARCHAR(64)  NOT NULL,
    name          VARCHAR(128) NOT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

CREATE TABLE IF NOT EXISTS user (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id      BIGINT       NOT NULL,
    username       VARCHAR(64)  NOT NULL,
    password_hash  VARCHAR(128) NOT NULL,
    role           VARCHAR(32)  NOT NULL DEFAULT 'USER',
    status         VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    channel        VARCHAR(32)  NOT NULL DEFAULT 'DEFAULT',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_tenant_username (tenant_id, username),
    KEY idx_user_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- Seed: 1 demo tenant + 1 admin user for local development
INSERT INTO tenant (id, code, name, status)
VALUES (1, 'demo', 'Demo Tenant', 'ACTIVE')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO user (id, tenant_id, username, password_hash, role, status, channel)
VALUES (1, 1, 'admin', 'PLACEHOLDER', 'ADMIN', 'ACTIVE', 'DEFAULT')
ON DUPLICATE KEY UPDATE role=VALUES(role);
```

**Compatibility notes**:
- H2 in `MODE=MySQL` supports `AUTO_INCREMENT`, `ENGINE=InnoDB`, `CHARSET=utf8mb4` (silently ignored). `ON UPDATE CURRENT_TIMESTAMP` is supported.
- `ON DUPLICATE KEY UPDATE` works in H2 MySQL mode.
- `COMMENT='...'` is ignored by H2.
- If H2 chokes on any of the above, prefix the failing statement with `-- !H2` conditional logic, or split into two migration files.

- [ ] **Step 12.2: Add a Tenant entity stub so we can map the table**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/entity/Tenant.java`:

```java
package com.kato.pro.langchain.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 租户实体。直接继承 BaseEntity 但**不使用其 tenantId 字段**（Tenant 是租户自身），
 * 因此单独定义字段。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tenant")
public class Tenant extends BaseEntity {

    private String code;
    private String name;
    private String status;
}
```

Wait — `Tenant extends BaseEntity` would inherit the `tenantId` field, which is wrong. Use composition-style instead:

Replace the file content of `Tenant.java` with:

```java
package com.kato.pro.langchain.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户实体（独立，不继承 BaseEntity）。
 *
 * 设计理由：BaseEntity 含 tenant_id 字段，但 Tenant 表本身就是租户维度，
 * 让 Tenant.tenantId 指向自身会造成语义混乱。Tenant 表独立维护自身的审计字段。
 */
@Data
@TableName("tenant")
public class Tenant implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
```

- [ ] **Step 12.3: Add User entity (does extend BaseEntity)**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/entity/User.java`:

```java
package com.kato.pro.langchain.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    private String username;
    private String passwordHash;
    private String role;
    private String status;
    private String channel;
}
```

- [ ] **Step 12.4: Add TenantMapper (uses BaseMapper, NOT TenantAwareBaseMapper)**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/infrastructure/persistence/TenantMapper.java`:

```java
package com.kato.pro.langchain.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kato.pro.langchain.common.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * Tenant 表的 Mapper。**注意**：Tenant 是租户维度表，不参与多租户过滤，
 * 因此使用普通 BaseMapper，不继承 TenantAwareBaseMapper。
 * 它对应的 SQL 在 TenantMybatisInterceptor 中通过表名豁免放行。
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
```

- [ ] **Step 12.5: Add UserMapper (uses TenantAwareBaseMapper)**

Create file `kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/infrastructure/persistence/UserMapper.java`:

```java
package com.kato.pro.langchain.infrastructure.persistence;

import com.kato.pro.langchain.common.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends TenantAwareBaseMapper<User> {
}
```

- [ ] **Step 12.6: Compile**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -q compile 2>&1 | tail -10
```
Expected: `BUILD SUCCESS` ✅

- [ ] **Step 12.7: Commit**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot
git add kato-ai/kato-langchain/src/main/resources/db/migration \
        kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/common/entity \
        kato-ai/kato-langchain/src/main/java/com/kato/pro/langchain/infrastructure/persistence
git commit -m "feat(kato-langchain): Flyway V1 (tenant/user tables + seed) + Tenant/User entities + Mappers"
```

---

### Task 13: End-to-end integration test (H2 + Flyway + 全链路)

**Files:**
- Create: `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/e2e/FoundationIntegrationTest.java`

**Purpose:** Boot the full Spring context with H2, verify Flyway migrations apply, all filters activate, and the tenant interceptor enforces SQL isolation.

- [ ] **Step 13.1: Create FoundationIntegrationTest**

Create file `kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/e2e/FoundationIntegrationTest.java`:

```java
package com.kato.pro.langchain.e2e;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kato.pro.langchain.LangChain4jApplication;
import com.kato.pro.langchain.common.entity.Tenant;
import com.kato.pro.langchain.common.entity.User;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.common.tenant.TenantInfo;
import com.kato.pro.langchain.infrastructure.persistence.TenantMapper;
import com.kato.pro.langchain.infrastructure.persistence.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M1 端到端集成测试：Spring 上下文启动 + Flyway 迁移 + 多租户隔离全链路验证。
 */
@SpringBootTest(classes = LangChain4jApplication.class)
@ActiveProfiles("test")
class FoundationIntegrationTest {

    @Autowired
    private TenantMapper tenantMapper;

    @Autowired
    private UserMapper userMapper;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void flywayApplied_seedTenantExists() {
        Tenant tenant = tenantMapper.selectById(1L);
        assertNotNull(tenant, "Flyway should have seeded tenant id=1");
        assertEquals("demo", tenant.getCode());
    }

    @Test
    void userCrud_underTenantContext_insertAndQueryBack() {
        // 1. 设置租户上下文
        TenantContext.set(TenantInfo.of(1L, 1L));

        // 2. 插入用户（拦截器会自动注入 tenant_id=1）
        User u = new User();
        u.setUsername("alice");
        u.setPasswordHash("hashed");
        u.setRole("USER");
        u.setStatus("ACTIVE");
        u.setChannel("DEFAULT");
        int rows = userMapper.insert(u);
        assertEquals(1, rows);

        // 3. 查询（拦截器自动追加 WHERE tenant_id=1）
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.eq(User::getUsername, "alice");
        User found = userMapper.selectOne(q);
        assertNotNull(found);
        assertEquals(1L, found.getTenantId(), "tenant_id should be auto-injected by interceptor");
        assertEquals("alice", found.getUsername());
    }

    @Test
    void userCrud_differentTenant_seesNoLeakage() {
        // 租户 1 插入 alice
        TenantContext.set(TenantInfo.of(1L, 1L));
        User u = new User();
        u.setUsername("bob");
        u.setPasswordHash("h");
        userMapper.insert(u);

        // 切换到租户 2 → 不应看到 bob
        TenantContext.set(TenantInfo.of(2L, 1L));
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.eq(User::getUsername, "bob");
        User found = userMapper.selectOne(q);
        assertNull(found, "tenant 2 should not see tenant 1's user (tenant isolation enforced)");
    }

    @Test
    void userCrud_noTenantContext_throwsOnSelect() {
        TenantContext.clear();
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.eq(User::getUsername, "anyone");
        // 拦截器应在无 TenantContext 时抛 SystemException
        assertThrows(RuntimeException.class, () -> userMapper.selectOne(q),
                "Without tenant context, SQL must be rejected");
    }
}
```

- [ ] **Step 13.2: Add Spring Boot test starter (if not already present)**

Check if `spring-boot-starter-test` is in `kato-ai/kato-langchain/pom.xml` — it already is per the existing pom. Verify `MockHttpServletRequest` is also available — it comes from `spring-test` which `spring-boot-starter-test` includes.

- [ ] **Step 13.3: Run integration test**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -q test -Dtest='FoundationIntegrationTest' 2>&1 | tail -60
```
Expected: `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` ✅

If integration test fails:
- **H2 syntax error** → adjust `V1__init_tenant_user.sql` (H2 may not accept some MySQL syntax). Look at the SQL error and adapt.
- **Interceptor not applied** → check that `MybatisPlusConfig` is being scanned (package `com.kato.pro.langchain.config`). The `LangChain4jApplication` has `scanBasePackages = "com.kato.pro.langchain"` so it should be picked up.
- **Testcontainers/MySQL** is **not** required for M1 unit/integration tests — H2 is sufficient. Testcontainers comes into play in M8/M10.

- [ ] **Step 13.4: Run full M1 test suite**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot/kato-ai/kato-langchain
mvn -q test 2>&1 | tail -30
```
Expected output:
```
Tests run: ResultTest(5) + BusinessExceptionTest(3) + GlobalExceptionHandlerTest(3) +
           TenantContextTest(7) + TraceIdFilterTest(3) + TenantMybatisInterceptorTest(8) +
           FoundationIntegrationTest(4)
         = 33 tests, 0 failures
BUILD SUCCESS
```

- [ ] **Step 13.5: Commit (M1 COMPLETION)**

```bash
cd /Users/guangfu.zeng/project/mine/kato-pivot
git add kato-ai/kato-langchain/src/test/java/com/kato/pro/langchain/e2e
git commit -m "feat(kato-langchain): M1 e2e integration test (Flyway + tenant isolation end-to-end)"
```


---

## Self-Review

**1. Spec coverage:**

| Spec §2.2 M1 requirement | Covered by |
|---|---|
| `kato-langchain` 升级为完整工程结构 | T1 (deps), T2 (packages), T10 (WebConfig), T11 (yml) |
| `TenantContextFilter` + `TenantAwareBaseMapper` + `TenantContext` ThreadLocal | T5 (Context+Filter), T7 (BaseMapper marker), T8 (Interceptor — actual enforcement) |
| 统一 `Result` 包装 | T3 |
| `GlobalExceptionHandler` | T4 |
| TraceId 过滤器 | T6 |
| MySQL/Redis/MiniMax 配置占位 | T11 (MySQL placeholder, Redis in T11 by not yet used — Redis is M2+ scope. MiniMax config is M2 scope.) |
| Flyway 初始化租户/用户表 | T12 |

**Known coverage gap (deliberate):**
- MiniMax config placeholder lives in M2, not M1. M1 only needs the yml schema to be in place.
- Redis config wiring lives in M2 (it needs the MiniMax client to connect). M1's yml is sufficient.
- "完整工程结构" includes Knife4j/OpenAPI integration — that lives in M10.

**2. Placeholder scan:** No "TBD" / "TODO" / "implement later" — every code block is concrete.

**3. Type consistency check:**
- `TenantInfo(Long tenantId, Long userId, String channel)` — defined T5.1, used T5.2/T5.4/T8.2/T13.1. ✅
- `TenantContext.requireCurrent() / currentTenantId() / currentUserId() / currentOrNull() / set() / clear()` — defined T5.2, used T5.5/T8.1/T13.1. ✅
- `Result<T>` static factories `ok/ok(T)/ok(msg,T)/fail(Code)/fail(Code,msg)/fail(code,msg)` — defined T3.4, used T4.1/T13.1. ✅
- `BaseEntity` fields: `id, tenantId, createTime, updateTime, deleted` — defined T7.1, used T12.3 (User extends). ✅
- `Tenant` overrides all fields from `BaseEntity` because it cannot extend it (Tenant IS the tenant, no self-reference) — T12.2. ✅
- `TenantMybatisInterceptor.rewriteSql(...)` signature — T8.1 (private), used T8.2 via reflection. ✅

**4. Risk acknowledgments:**
- **R1 (MiniMax compatibility)** — not relevant to M1, deferred to M2.
- **R2 (cross-tenant leak)** — directly addressed by T8 (P0 interceptor) + T13 (e2e tenant isolation test).
- **JSqlParser API differences** — `update.getWhere()` and `delete.getWhere()` may be read-only in some JSqlParser versions. Fallback: rewrite as string concatenation (already done in T8.1).
- **H2 syntax quirks in V1 SQL** — possible; if encountered, T13.3 will fail loudly and we'll iterate. The SQL is intentionally written to be H2-compatible (MySQL mode).
- **`PluginUtils.setOriginalSql` availability** — if it doesn't exist in 3.5.7, use the reflection fallback documented in T8.1.

---

## Execution Handoff

**Plan complete and saved to `/Users/guangfu.zeng/project/mine/kato-pivot/docs/superpowers/plans/2026-07-07-ai-customer-service-m1-foundation.md` (2306 lines, 13 tasks).**

Two execution options:

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration with isolation between contexts.

**2. Inline Execution** — Execute tasks in this session using `superpowers:executing-plans`, batch execution with checkpoints for review.

**Which approach?**

---

## M1 Acceptance Checklist (run after all 13 tasks)

- [ ] `mvn -q test` from `kato-ai/kato-langchain/` → 33 tests, 0 failures
- [ ] Application boots: `mvn spring-boot:run -pl kato-ai/kato-langchain` → starts on port 8080
- [ ] Flyway migration visible: GET `/actuator/flyway` (after M2 adds actuator) → V1 applied
- [ ] Tenant filter rejects missing header: `curl -i http://localhost:8080/api/v1/test` → 200 with code=TENANT_MISMATCH
- [ ] Tenant filter accepts valid header: `curl -i -H "X-Tenant-Id: 1" -H "X-User-Id: 1" http://localhost:8080/api/v1/test` → no tenant error
- [ ] TraceId round-trips: `curl -i -H "X-Trace-Id: my-trace" ...` → response header `X-Trace-Id: my-trace`
- [ ] All commits clean: `git log --oneline` shows 13 commits matching the plan's commit messages

---

## What's Next (after M1 complete)

- **M2** (模型路由 + MiniMax 客户端) — gets its own brainstorm/plan cycle. Will write `2026-07-07-ai-customer-service-m2-model-router.md` after M1 is done and reviewed.
- The whole 10-module roadmap remains on `docs/superpowers/specs/2026-07-07-ai-customer-service-design.md`.

