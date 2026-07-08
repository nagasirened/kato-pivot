# kato-risk 工程化设计文档

> Phase 1 工程化骨架设计，基于风控系统设计文档 v2026-05-30

---

## 1. Maven 模块结构

### 1.1 父 pom：`kato-risk/pom.xml`

```
kato-risk (pom)
├── kato-risk-client/    # 轻量 SDK，业务系统引入
└── kato-risk-server/   # 规则执行服务，独立部署
```

**继承关系**：`kato-risk` → `kato-pivot`（复用 dependencyManagement）

**子模块声明**：
```xml
<modules>
    <module>kato-risk-client</module>
    <module>kato-risk-server</module>
</modules>
```

**Phase 1 dependencyManagement 扩展**：

| dependency | groupId | artifactId | 版本来源 |
|---|---|---|---|
| resilience4j-bom | io.github.resilience4j | resilience4j-bom | kato-pivot bom import |
| groovy | org.codehaus.groovy | groovy | 新增（脚本执行引擎） |
| spring-kafka | org.springframework.kafka | spring-kafka | kato-pivot bom |
| mybatis-plus-boot-starter | com.baomidou | mybatis-plus-boot-starter | kato-pivot bom |
| caffeine | com.github.ben-manes.caffeine | caffeine | kato-pivot bom |
| kato-resilience4j | org.kato.pro | kato-resilience4j | 项目内部模块复用 |
| kato-redis | org.kato.pro | kato-redis | 项目内部模块复用 |
| kato-web | org.kato.pro | kato-web | 项目内部模块复用 |

---

## 2. 包结构设计

### 2.1 kato-risk-client：`com.kato.pro.risk.client`

```
com.kato.pro.risk.client
├── dto
│   ├── RiskRequest.java          # 风控请求（场景/用户/设备/IP/业务数据）
│   ├── RiskResponse.java         # 风控响应（action/score/reasonCodes）
│   └── BusinessDataProvider.java # 业务数据查询接口（业务方实现）
├── feign
│   ├── RiskFeignClient.java      # 调用风控 Server 的 FeignClient
│   └── FallbackRiskFeignClient.java # 熔断降级实现
├── config
│   ├── RiskClientConfig.java     # Feign + 熔断 + 缓存配置
│   └── CaffeineConfig.java       # Caffeine 本地缓存配置
├── service
│   └── RiskClientService.java    # 对外暴露的风控检查门面
└── constant
    ├── RiskScene.java           # 场景枚举（REGISTER/LOGIN/ORDER/PAYMENT/REFUND）
    ├── RiskAction.java          # 决策枚举（PASS/REVIEW/BLOCK）
    └── ReasonCode.java          # 原因码枚举
```

### 2.2 kato-risk-server：`com.kato.pro.risk.server`

```
com.kato.pro.risk.server
├── controller                     # REST API
│   ├── RiskCheckController.java   # /risk/check
│   ├── RuleController.java        # /risk/rule CRUD
│   └── GroovyScriptController.java # /risk/script
├── service                        # 业务逻辑
│   ├── RuleEngineService.java     # 规则执行引擎
│   ├── RuleConfigService.java     # 配置规则 SpEL 解析执行
│   ├── GroovyScriptService.java   # Groovy 脚本加载/执行/缓存
│   ├── BusinessDataQueryService.java # 查询业务方数据
│   └── CaseService.java           # 案件管理
├── engine
│   ├── RuleExecutor.java          # 规则执行器接口
│   ├── ConfigRuleExecutor.java    # 配置规则执行（SpEL）
│   └── GroovyRuleExecutor.java    # Groovy 脚本执行
├── groovy
│   ├── GroovyScriptLoader.java   # 脚本从 MySQL 加载到本地缓存
│   ├── GroovyScriptCompiler.java  # Groovy AST 安全扫描 + 编译
│   └── RiskCheckContext.java     # Groovy 脚本入参上下文
├── kafka
│   ├── RuleUpdateConsumer.java   # Kafka Consumer 热更新监听
│   └── RuleUpdateProducer.java   # Kafka Producer（admin 侧）
├── entity                         # 数据库实体
│   ├── RiskRule.java
│   ├── RiskCase.java
│   ├── RiskRejectLog.java
│   └── RiskRuleAudit.java
├── mapper
│   ├── RiskRuleMapper.java
│   ├── RiskCaseMapper.java
│   └── RiskRejectLogMapper.java
├── dto
│   ├── RiskRuleDTO.java
│   ├── GroovyTestRequest.java
│   └── GroovyTestResult.java
└── config
    ├── KafkaConfig.java
    ├── GroovyConfig.java         # Groovy Shell 安全配置
    └── Resilience4jConfig.java
```

---

## 3. Phase 1 核心类骨架（3 个）

### 3.1 RiskFeignClient.java（client）

```java
package com.kato.pro.risk.client.feign;

import com.kato.pro.risk.client.dto.RiskRequest;
import com.kato.pro.risk.client.dto.RiskResponse;
import com.kato.pro.risk.client.dto.GroovyTestRequest;
import com.kato.pro.risk.client.dto.GroovyTestResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 业务系统调用风控 Server 的 FeignClient 接口。
 * Phase 1 仅定义骨架，不实现调用逻辑。
 */
@FeignClient(
    name = "kato-risk-server",
    url = "${risk.server.url:http://localhost:8080}",
    fallback = FallbackRiskFeignClient.class
)
public interface RiskFeignClient {

    @PostMapping("/risk/check")
    RiskResponse check(@RequestBody RiskRequest request);

    @PostMapping("/risk/check/groovy")
    GroovyTestResult testGroovyScript(@RequestBody GroovyTestRequest request);
}
```

### 3.2 BusinessDataProvider.java（client）

```java
package com.kato.pro.risk.client.dto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 业务数据查询接口。定义在 kato-risk-client，业务方实现并注册为 Spring Bean。
 * Phase 1 仅定义骨架，接口方法签名根据设计文档 scene 对应业务数据需求。
 */
@FeignClient(name = "business-data-provider")
public interface BusinessDataProvider {

    @GetMapping("/internal/user/{userId}/recent-orders")
    List<Object> getRecentOrders(@PathVariable("userId") String userId,
                                 @RequestParam("days") int days);

    @GetMapping("/internal/user/{userId}/addresses")
    List<Object> getUserAddresses(@PathVariable("userId") String userId);

    @GetMapping("/internal/user/{userId}/refund-records")
    List<Object> getRefundRecords(@PathVariable("userId") String userId,
                                  @RequestParam("days") int days);

    @GetMapping("/internal/user/{userId}/login-history")
    List<Object> getLoginHistory(@PathVariable("userId") String userId,
                                  @RequestParam("days") int days);
}
```

### 3.3 RuleEngineService.java（server）

```java
package com.kato.pro.risk.server.service;

import com.kato.pro.risk.client.dto.RiskRequest;
import com.kato.pro.risk.client.dto.RiskResponse;

/**
 * 规则执行引擎接口。
 * Phase 1 核心方法骨架：
 *   1. scene 白名单校验
 *   2. 规则匹配（按 scene + priority）
 *   3. 配置规则 SpEL 执行
 *   4. Groovy 脚本执行
 *   5. 风险分数加权 → RiskResponse
 *
 * 具体实现（SpEL/Groovy/缓存/熔断）Phase 2 展开。
 */
public interface RuleEngineService {

    /**
     * 执行风控检查。
     *
     * @param request 风控请求上下文
     * @return 风控决策响应
     */
    RiskResponse check(RiskRequest request);

    /**
     * 热更新触发：重新加载指定规则。
     *
     * @param ruleId 规则 ID
     * @param version 新版本号
     */
    void reloadRule(Long ruleId, Integer version);
}
```

---

## 4. 关键设计决策

| 决策点 | 方案 | 依据 |
|---|---|---|
| parent pom | 直接继承 kato-pivot，不经过 kato-core | 减少层级，dependencyManagement 拉平 |
| Resilience4j | 复用 kato-resilience4j 模块 | 项目已有，避免重复引入 |
| Groovy 依赖 | 新增 groovy-all（版本由 dependencyManagement 管理） | 脚本执行引擎，kato-pivot 无此依赖 |
| Kafka Topic | 独立 topic `risk-rule-update` | 设计文档 6.Q3 已确认，不复用 sensitive-word-update |
| 缓存策略 | Caffeine（本地）+ Redis（分布式） | 同 kato-sensitive 架构 |
| gRPC vs HTTP | Phase 1 使用 Spring Cloud OpenFeign HTTP 调用 | 降低复杂度，gRPC 在 Phase 2+ 按需引入 |

---

## 5. 需 P8 确认事项

1. **Groovy 版本**：建议使用 `3.0.9`（兼容 Java 8，AST 安全扫描能力成熟），是否OK？
2. **BusinessDataProvider 接口**：设计文档列出 4 个方法，Phase 1 是否先实现 `getRecentOrders` 和 `getUserAddresses`，其他按需扩展？
3. **Phase 1 验收标准**：注册或登录环节规则跑通 + Groovy 脚本存储/执行，是否作为 Phase 1 交付门槛？

