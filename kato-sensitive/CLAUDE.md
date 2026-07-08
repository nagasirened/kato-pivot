# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

敏感词过滤系统。client-server 双服务 + 独立 Vue 3 管理后台，通过 Kafka 实现服务端→客户端的敏感词热更新。父模块 `kato-pivot` 提供基础依赖（`kato-web`、`kato-base`），具体依赖版本和分层约定见父仓库 `CLAUDE.md` 和 `.claude/CLAUDE.md`。

## Build & Dev Commands

```bash
# 在父仓库 kato-pivot/ 下执行，或先 cd 到本模块

# 全模块构建
mvn clean install -DskipTests -pl kato-sensitive -am

# 单独构建 server / client
mvn clean install -DskipTests -pl kato-sensitive/kato-sensitive-server -am
mvn clean install -DskipTests -pl kato-sensitive/kato-sensitive-client -am

# 运行（注意端口：server=8086, client=8087，与父 CLAUDE.md 中的 8080/8081 不一致）
mvn spring-boot:run -pl kato-sensitive/kato-sensitive-server
mvn spring-boot:run -pl kato-sensitive/kato-sensitive-client

# 启动管理后台
cd kato-sensitive-ui && npm install && npm run dev    # Vite, 端口 3000

# 测试（稀疏，不要轻易声称通过）
mvn test -pl kato-sensitive/kato-sensitive-server
mvn test -pl kato-sensitive/kato-sensitive-client
```

> 父 `kato-pivot/CLAUDE.md` 里写的是 server=8080 / client=8081，但 `application.yml` 实际是 8086 / 8087。改端口前确认所有上游依赖。

## 模块结构

```
kato-sensitive/                              # 父 pom (packaging=pom)
├── kato-sensitive-server/                   # 管理端 — 敏感词 CRUD、Excel、审核
├── kato-sensitive-client/                   # 检测端 — DFA/Trie/Regex 匹配 + 熔断
├── kato-sensitive-ui/                       # Vue 3 + Element Plus 管理后台（自带 CLAUDE.md）
└── docs/sensitive-system.md                 # 系统设计文档（与本文档部分重叠）
```

两个 Java 模块都打包成独立 Spring Boot jar，可独立部署。模块间不直接依赖 Java 代码，只通过 **Kafka 主题 `sensitive-word-update`** 同步敏感词变更。

## 关键架构

### 1. 服务端 → 客户端的热更新链路

```
server: CRUD/Excel导入
  ↓ SensitiveWordServiceImpl.save/update/removeById → sendKafkaNotice()
Kafka topic: sensitive-word-update (string payload "update")
  ↓
client: SensitiveWordKafkaConsumer.onMessage()
  → SensitiveWordLoadServiceImpl.hotReloadSensitiveWords()
  → 清空 Trie/DFA/Regex → 重新从 DB 加载 status=ENABLE 的词
```

- 服务端发送的 Kafka 消息**不携带具体变更内容**，只是个"reload" 信号；客户端每次都全量重查 DB。
- `SensitiveWordLoadServiceImpl` 持有 `volatile long version` 用于追踪重载次数。
- 服务端也提供 `POST /sensitive/v1/word/notify` 手动触发 Kafka 通知。
- `@PostConstruct` 启动时同步执行首次加载，加载失败仅记日志（不阻断启动）。

### 2. 匹配引擎（三套并存，按 wordType 分流）

| 算法 | 文件 | 输入 | 用途 |
|------|------|------|------|
| DFA | `dfa/SensitiveWordDFA.java` | EXACT 类型词 | 高效精确匹配 |
| Trie | `trie/SensitiveWordTrie.java` | EXACT 类型词 | 模糊匹配底座 |
| Regex | `algorithm/RegexMatchAlgorithm.java` | REGEX 类型词 | 复杂模式 |

- 检测接口 `POST /sensitive/v1/check` 通过 `wordType` (EXACT/REGEX/ALL) 决定走哪条路径。
- "ALL" 模式 = DFA + Regex + 可选 Fuzzy（命中成本最高，慎用）。
- 三套算法共享同一份加载逻辑（`SensitiveWordLoadServiceImpl.loadSensitiveWords`），按 `wordType` 分桶喂入。

### 3. 模糊匹配（仅 EXACT 词触发）

`CharacterConverter.java` 提供三种变体生成：
- **简繁转换**：`toTraditional()` —— `爱国 → 愛國`
- **同音字**：`generateHomophoneVariants()` —— `我 → 哦/卧`
- **形近字**：`generateSimilarVariants()` —— `天 → 夫/夭`

每种变体都用 Trie `matchContains()` 扫一遍原文。**性能陷阱**：变体生成会爆炸式增长文本长度，对长文本禁用 fuzzy（或预 filter 短文本）。

### 4. 缓存策略（双 Caffeine，TTL 故意不同）

```java
cleanCache: 30秒过期 / 10000条   // 无命中结果（占大头）
hitCache:   5分钟过期 / 5000条   // 有命中结果（怕命中词被改）
```

cacheKey = `text.hashCode() + "_" + fuzzyMatch`（或 `"_exact"`），注意 hash 碰撞不是问题（命中错也只多花一次匹配时间）。

### 5. 熔断降级（Resilience4j）

所有检测入口（`check`/`checkExact`/`checkByLevel`/`checkFuzzy`）都包了 `@CircuitBreaker(name = "sensitiveCheck", fallbackMethod = ...)`，配置在 `Resilience4jConfig.java`。

降级返回 `hasSensitive=false, fallback=true` —— **默认放行所有文本**。调用方必须读 `fallback` 标志识别降级状态，不能只靠 `hasSensitive`。

### 6. 响应统一结构

所有 controller 返回 `com.kato.pro.base.entity.Result<T>`（来自 `kato-base`）。错误用 `Result.build(code, message)` 形式：

```java
return Result.build(1, "文本不能为空");
return Result.build(result);                  // 成功，code=0
```

DTO/Entity 字段命名沿用项目约定（蛇形），但 Java 类字段是驼峰，靠 MyBatis-Plus 的 `map-underscore-to-camel-case` 自动映射。

## 数据模型

`sensitive_word` 表（实体 `SensitiveWord` 继承 `SuperModel`，含 createTime/updateTime/isDeleted）：

| 字段 | 枚举值 |
|------|--------|
| `word` | 字符串 |
| `level` | `SensitiveLevel`: URGENT / MEDIUM / NORMAL |
| `status` | `SensitiveStatus`: ENABLE / DISABLE（DB 查 ENABLE 喂匹配器） |
| `category` | `SensitiveCategory`: POLITICS / PORN / AD / VIOLENCE / FRAUD / OTHER |
| `wordType` | `WordType`: EXACT / REGEX（决定走 DFA 还是 Regex） |

> 等级 URGENT/MEDIUM/NORMAL 数值化在 `SensitiveCheckServiceImpl.getLevelValue()`（3/2/1）里硬编码，新增等级需同步改这里。`checkByLevel` 用此值做"大于等于"过滤。

> 服务端和客户端各自维护一套 `SensitiveLevel` / `SensitiveCategory` 枚举（同一个包路径，但 Maven 模块不同——其实是同一份代码的两次编译）。改枚举值会同时影响两边。

## 审核流程（仅 client 暴露接口）

| 端点 | 用途 |
|------|------|
| `POST /sensitive/v1/audit/apply` | 提交待审核词 |
| `GET  /sensitive/v1/audit/page` | 待审列表 |
| `POST /sensitive/v1/audit/approve` | 通过 |
| `POST /sensitive/v1/audit/reject` | 拒绝 |

`SensitiveWordAudit` 实体（仅 client 持有），状态 `AuditStatus`: PENDING / APPROVED / REJECTED。

## Excel 导入导出（仅 server）

- 用阿里 EasyExcel 4.0.3（pom 中**显式锁定版本**，不要随意升）
- 导入：`POST /sensitive/v1/word/import` + multipart file → `SensitiveWordImportListener` → `batchSave`
- 导出：`GET /sensitive/v1/word/export` + 可选 `SensitiveWord` 查询参数 → 流式写到 response
- 模板下载：`GET /sensitive/v1/word/template`（生成单行示例 .xlsx）
- `DTO` ↔ `Entity` 转换在 `SensitiveWordController.convertToDTO()` 里手动写，没用 MapStruct

## 操作日志

服务端维护 `sensitive_operation_log` 表，记录 CRUD 前后数据（`SensitiveOperationLog`）。当前 `SensitiveWordServiceImpl` 显式发送 Kafka 但**未写入日志**——controller 也没调。日志接口 `GET /sensitive/v1/word/log/page` 可查，但目前没数据来源，待补。

## 客户端自己的数据库表

除 `sensitive_word`（与服务端共享一份 DB schema）外，client 还独有：
- `sensitive_word_hit_log` —— 每次检测的命中日志（文本长度、命中数、等级、分类、响应时间）
- `sensitive_word_audit` —— 审核流程

> 注意：server 和 client 都配 `datasource.url = jdbc:mysql://localhost:3306/kato_sensitive`，**共用同一份 MySQL**。改表结构需同时考虑两端。

## 关键代码定位

| 想找什么 | 看这个文件 |
|---------|-----------|
| 检测主流程 | `kato-sensitive-client/.../service/impl/SensitiveCheckServiceImpl.java` |
| 热更新触发 | `kato-sensitive-client/.../kafka/SensitiveWordKafkaConsumer.java` |
| 全量重载逻辑 | `kato-sensitive-client/.../service/impl/SensitiveWordLoadServiceImpl.java` |
| DFA 匹配 | `kato-sensitive-client/.../dfa/SensitiveWordDFA.java` |
| Trie 匹配 | `kato-sensitive-client/.../trie/SensitiveWordTrie.java` |
| 模糊变体生成 | `kato-sensitive-client/.../util/CharacterConverter.java` |
| 字符转换辅助 | `kato-sensitive-client/.../util/PinyinUtil.java` |
| Kafka 通知发送 | `kato-sensitive-server/.../service/impl/SensitiveWordServiceImpl.java` (`sendKafkaNotice`) |
| Excel 导入监听 | `kato-sensitive-server/.../listener/SensitiveWordImportListener.java` |
| 脱敏 | `kato-sensitive-client/.../service/impl/SensitiveSanitizerImpl.java` |

## 已知事项 / 踩坑

- **DB 共用**：server 和 client 连同一个 `kato_sensitive` 库，但代码上不存在 JOIN/外键。改表结构前对齐两端。
- **`maven.compiler.source=8` 在根 pom，但 server 子 pom 强制覆盖为 16**：本模块实际编译目标是 Java 16。修改时别被根 pom 的 8 误导。
- **审计日志功能未接通**：见上面"操作日志"小节，导入/批量保存时不写日志。
- **Kafka 消息不带内容**：客户端永远全量重查。如果词表上百万条会成为热点，需改成增量 diff 推送。
- **检测高耗时风险**：`check()` 同步串联 DFA+Regex+Fuzzy，单次最坏可能上百毫秒。配合 Caffeine 缓存能挡住热点但兜不住突发。
- **回滚风险**：`sendKafkaNotice()` 在 CRUD 事务外异步发送；如果 CRUD 因事务回滚失败但 Kafka 已发出，会导致客户端重新加载"旧数据"——客户端每次全量查 DB 所以最终一致，但会有短暂回滚感知。
- **测试覆盖稀疏**：mvn test 经常 0 测试通过，不要轻易报"已验证"。

## UI 子模块

`kato-sensitive-ui/` 是独立的 Vue 3 SPA，**不参与 Maven 构建**。双 axios 实例分别打 server (8086) 和 client (8087)。详见该子目录自己的 `CLAUDE.md`。