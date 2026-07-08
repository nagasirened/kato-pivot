# 敏感词系统 (kato-sensitive)

## 1. 系统概述

敏感词系统是一个用于文本内容安全检测的分布式服务，采用微服务架构，分为服务端(`kato-sensitive-server`)和客户端(`kato-sensitive-client`)两个模块。

**核心能力：**
- 多模式匹配：DFA精确匹配、正则匹配、模糊匹配（同音字/形近字/简繁体）
- 热更新机制：通过Kafka消息队列实现敏感词实时更新
- 降级保护：集成Resilience4j熔断器，防止系统雪崩
- 审核流程：支持敏感词的提交、审批、拒绝等审核管理

## 2. 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         客户端 (kato-sensitive-client)          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │ Trie树      │  │ DFA算法     │  │ 正则匹配    │  匹配引擎   │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │ Caffeine    │  │ Redis缓存   │  │ Kafka消费者 │  辅助组件  │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────────────────────────────────────────────────┘
                              ↑
                    Kafka消息通知
                              ↑
┌─────────────────────────────────────────────────────────────────┐
│                        服务端 (kato-sensitive-server)            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │ 敏感词管理   │  │ 审核流程    │  │ Excel导入/导出│ 管理功能  │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │ MySQL       │  │ Kafka生产者  │  │ 操作日志    │  数据存储  │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────────────────────────────────────────────────┘
```

## 3. 敏感词数据模型

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| word | String | 敏感词内容 |
| level | String | 敏感等级：URGENT(紧急)、MEDIUM(中等)、NORMAL(普通) |
| status | String | 状态：ENABLE(启用)、DISABLE(禁用) |
| category | String | 分类：POLITICS(政治)、PORN(色情)、AD(广告)、VIOLENCE(暴恐)、FRAUD(诈骗)、OTHER(其他) |
| word_type | String | 词类型：EXACT(精确匹配)、REGEX(正则匹配) |
| remark | String | 备注 |

## 4. 核心功能

### 4.1 敏感词检测

**API端点：** `POST /sensitive/v1/check`

**请求参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| text | String | 待检测文本 |
| fuzzyMatch | boolean | 是否启用模糊匹配（默认false） |
| wordType | String | 检测模式：EXACT/REGEX/ALL |
| sanitize | boolean | 是否返回脱敏文本 |
| maskChar | char | 脱敏替换字符（默认*） |

**匹配算法：**
1. **DFA精确匹配**：时间复杂度O(n)，与敏感词数量无关
2. **正则匹配**：支持复杂模式，如`微[信信]+`可匹配"微信""微信号"
3. **模糊匹配**：
   - 同音字替换：`我`→`哦`、`卧`
   - 形近字替换：`天`→`夫`、`夭`
   - 简繁转换：`爱国`→`愛國`

### 4.2 敏感词管理（服务端）

| 功能 | 端点 | 说明 |
|------|------|------|
| 分页查询 | `GET /sensitive/v1/word/page` | 支持按词、等级、状态、分类筛选 |
| 增删改 | `POST/DELETE/PUT` | CRUD操作 |
| Excel导入 | `POST /sensitive/v1/word/import` | 批量导入敏感词 |
| Excel导出 | `GET /sensitive/v1/word/export` | 按条件导出敏感词 |
| 下载模板 | `GET /sensitive/v1/word/template` | 获取导入模板 |
| 操作日志 | `GET /sensitive/v1/word/log/page` | 查看敏感词变更记录 |

### 4.3 审核流程（客户端）

| 功能 | 端点 | 说明 |
|------|------|------|
| 提交审核 | `POST /sensitive/v1/audit/apply` | 提交新敏感词待审核 |
| 待审核列表 | `GET /sensitive/v1/audit/page` | 查看待审批的敏感词 |
| 审批通过 | `POST /sensitive/v1/audit/approve` | 审核人批准敏感词 |
| 审批拒绝 | `POST /sensitive/v1/audit/reject` | 审核人拒绝敏感词 |

**审核状态：** PENDING(待审核) → APPROVED(已通过) / REJECTED(已拒绝)

### 4.4 脱敏服务

**API端点：** 内嵌于检测接口（`sanitize=true`时返回）

支持按等级脱敏：
- URGENT：全部替换为指定字符
- MEDIUM：部分替换
- NORMAL：不替换（可配置）

## 5. 技术组件

### 5.1 匹配引擎

| 组件 | 文件 | 说明 |
|------|------|------|
| Trie树 | `SensitiveWordTrie.java` | 前缀树结构，支持包含匹配 |
| DFA过滤器 | `SensitiveWordDFA.java` | 确定有限自动机，高效精确匹配 |
| 正则匹配 | `RegexMatchAlgorithm.java` | 处理复杂正则模式 |

### 5.2 字符转换工具

`CharacterConverter.java` 提供：
- 简繁体互转
- 同音字生成
- 形近字生成
- 特殊符号过滤

### 5.3 缓存策略

| 缓存类型 | 配置 | 用途 |
|----------|------|------|
| Caffeine | 30秒过期，最大10000条 | 无敏感词结果缓存 |
| Caffeine | 5分钟过期，最大5000条 | 有敏感词结果缓存 |
| 分类缓存 | 每小时刷新 | 按分类存储敏感词 |

### 5.4 熔断保护

使用Resilience4j配置熔断器：
- 触发条件：连续失败超过阈值
- 降级策略：返回`hasSensitive=false`
- 自动恢复：失败率降低后自动开启

## 6. 热更新机制

```
敏感词变更 → Kafka消息 → 客户端Kafka消费者 → 重新加载敏感词到内存
```

1. 服务端敏感词变更后，发送Kafka消息到`sensitive-word-update`主题
2. 客户端监听该主题，收到消息后触发`hotReloadSensitiveWords()`
3. 重新从数据库加载启用的敏感词，更新Trie/DFA/正则匹配器

## 7. 日志记录

| 日志类型 | 说明 |
|----------|------|
| 命中日志 | 记录每次检测的文本长度、命中数量、等级、分类、响应时间 |
| 操作日志 | 记录敏感词的增删改操作，含操作人、操作类型、操作前后数据 |

## 8. API响应格式

```json
{
  "code": 0,
  "data": {
    "hasSensitive": true,
    "matchCount": 2,
    "maxLevel": "URGENT",
    "categories": ["POLITICS", "FRAUD"],
    "matches": [
      {
        "word": "诈骗",
        "level": "MEDIUM",
        "matchType": "EXACT",
        "startIndex": 5,
        "endIndex": 7
      }
    ],
    "sanitizedText": "请不要***",
    "fallback": false
  }
}
```

## 9. 依赖技术

- **框架**：Spring Boot + MyBatis-Plus
- **注册中心**：Nacos/Consul（通过@EnableDiscoveryClient）
- **消息队列**：Kafka
- **缓存**：Caffeine + Redis
- **熔断**：Resilience4j
- **Excel处理**：EasyExcel
- **数据库**：MySQL

## 10. 模块结构

```
kato-sensitive/
├── pom.xml                    # 父模块
├── kato-sensitive-server/     # 服务端模块
│   └── src/main/java/com/kato/pro/sensitive/
│       ├── SensitiveServerApplication.java
│       ├── controller/        # 管理接口
│       ├── service/           # 业务逻辑
│       ├── mapper/            # 数据访问
│       ├── entity/           # 数据实体
│       ├── listener/         # Excel导入监听
│       └── config/           # Kafka等配置
└── kato-sensitive-client/     # 客户端模块
    └── src/main/java/com/kato/pro/sensitive/
        ├── SensitiveClientApplication.java
        ├── controller/        # 检测接口
        ├── service/          # 核心检测服务
        ├── dfa/              # DFA算法
        ├── trie/             # Trie树
        ├── algorithm/        # 正则匹配
        ├── util/             # 字符转换
        ├── kafka/            # Kafka消费者
        └── config/           # 缓存、熔断配置
```