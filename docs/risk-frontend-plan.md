# 风控系统前端方案

## 决策已确认

| 决策 | 选择 | 说明 |
|------|------|------|
| D1 | ✅ A) 补充 REST API | 新增管理接口 `/api/risk/xxx` |
| D2 | ✅ A) Ant Design 5 + 企业蓝 | 与 `kato-sensitive-ui` 一致 |
| D3 | ✅ A) 独立工程 `kato-risk-web` | 职责分离，独立部署 |

---

## 一、系统定位

风控系统面向两类用户：
1. **运营/策略人员** — 配置规则、测试脚本、查看风险事件
2. **开发/运维人员** — 监控服务状态、调整场景开关

---

## 二、功能模块

### 1. 仪表盘（Dashboard）

入口页面，展示整体风险态势。

**数据指标：**
- 今日检查量 / 通过率 / 拦截率
- 各场景（REGISTER/LOGIN/ORDER/PAYMENT/REFUND）检查量分布
- 拦截趋势折线图（近7天）
- TOP 触发规则排行

**布局：**
- 顶部：4个核心指标卡片（今日检查、通过、拦截、待审）
- 中部：拦截趋势图（折线）+ 场景分布（饼图）
- 底部：TOP 规则触发列表

---

### 2. 规则管理（Rule Management）

**规则列表页：**
- 表格字段：ID、规则名称、场景、类型、优先级、启用状态、版本、操作
- 筛选：场景（多选）、类型（CONFIG/GROOVY）、启用状态
- 操作：新建、编辑、复制、启用/禁用、删除

**规则编辑页：**
- 基本信息：名称、场景（单选）、类型（切换）、优先级
- 规则内容编辑器：
  - CONFIG 类型：SpEL 表达式文本框（语法高亮）
  - GROOVY 类型：Groovy 脚本编辑器（语法高亮）
- A/B 分组配置
- 底部的"测试脚本"按钮 → 跳转 Groovy 测试页

---

### 3. Groovy 脚本测试（Script Testing）

独立页面，用于验证脚本正确性后再保存到规则。

**输入：**
- 场景选择（决定可用的上下文变量）
- 输入上下文 KV 表单（userId, deviceId, ip, orderAmount 等）
- 脚本内容编辑器（Groovy 语法高亮）

**输出：**
- 执行结果：action / score / reasonCode
- 执行耗时（ms）
- 成功/失败状态

---

### 4. 风险事件（Case Management）

**事件列表页：**
- 表格字段：请求ID、用户ID、场景、决策（PASS/BLOCK/REVIEW）、风险分、操作时间
- 筛选：场景、决策、日期范围、用户ID
- 操作：查看详情

**事件详情页：**
- 请求信息：userId / deviceId / ip / scene / requestId
- 规则命中情况：触发了哪些规则、每个规则的输出
- 最终决策：action / score / reasonCodes
- 关联订单信息（如有）

---

### 5. 审核日志（Audit Log）

规则变更历史，强制审计。

**表格字段：** 操作时间、操作人、规则名称、变更类型（创建/修改/删除）、版本、变更前后内容对比

**变更内容对比：** diff 视图展示 rule content 变化

---

### 6. 场景配置（Scene Config）

全局开关，控制各场景启用/停用。

**配置项：**
- REGISTER / LOGIN / ORDER / PAYMENT / REFUND 各场景启用/停用
- 每个场景可设置默认放行策略（PASS/BLOCK）

---

## 三、技术选型

| 层次 | 选择 | 说明 |
|------|------|------|
| 框架 | React 18 + Vite | 主流，快速迭代 |
| UI 组件库 | Ant Design 5 | 企业级，规则管理场景成熟 |
| 状态管理 | Zustand | 轻量，业务简单 |
| 路由 | React Router v6 | 标准 |
| 图表 | ECharts | 拦截趋势、场景分布 |
| 脚本编辑器 | @monaco-editor/react | Groovy/SpEL 语法高亮 |
| HTTP | Axios + TanStack Query | 数据获取 + 缓存 |
| 部署 | 独立前端工程，打包后 Nginx 部署 | 与后端解耦 |

**后端 REST API（需补充开发）：**
| 接口 | 说明 |
|------|------|
| `GET/POST/PUT/DELETE /api/risk/rules` | 规则 CRUD |
| `POST /api/risk/rules/test` | Groovy 脚本测试 |
| `GET /api/risk/cases` | 事件列表 |
| `GET /api/risk/cases/:id` | 事件详情 |
| `GET /api/risk/audit-logs` | 审核日志 |
| `GET /api/risk/stats/dashboard` | 仪表盘数据 |
| `PUT /api/risk/scenes/:scene/toggle` | 场景开关 |

---

## 四、项目结构 `kato-risk-web`

```
kato-risk-web/
├── public/
├── src/
│   ├── api/               # Axios 请求封装
│   │   └── risk.ts        # 风控相关 API
│   ├── components/        # 公共组件
│   │   ├── Header.tsx
│   │   ├── RuleEditor.tsx
│   │   └── ScriptEditor.tsx
│   ├── pages/
│   │   ├── Dashboard/      # 仪表盘
│   │   ├── RuleList/      # 规则列表
│   │   ├── RuleEdit/      # 规则编辑
│   │   ├── ScriptTest/    # 脚本测试
│   │   ├── CaseList/      # 风险事件
│   │   ├── CaseDetail/    # 事件详情
│   │   ├── AuditLog/      # 审核日志
│   │   └── SceneConfig/   # 场景配置
│   ├── stores/            # Zustand store
│   ├── router/            # 路由配置
│   └── styles/            # 全局样式
├── package.json
├── vite.config.ts
└── index.html
```

---

## 五、页面清单

| # | 页面 | 路由 | 说明 |
|---|------|------|------|
| 1 | 仪表盘 | `/` | 概览数据 |
| 2 | 规则列表 | `/rules` | 规则管理入口 |
| 3 | 规则编辑 | `/rules/new`, `/rules/:id/edit` | 新建/编辑规则 |
| 4 | 脚本测试 | `/rules/test` | Groovy 在线测试 |
| 5 | 风险事件 | `/cases` | 事件查询 |
| 6 | 事件详情 | `/cases/:id` | 单条事件详情 |
| 7 | 审核日志 | `/audit-logs` | 变更历史 |
| 8 | 场景配置 | `/scene-config` | 全局开关 |

---

## 六、开发优先级

**Phase 1（MVP）：**
- 仪表盘 + 规则列表 + 规则编辑 + 脚本测试
- 同时开发对应的后端 REST API

**Phase 2（完善）：**
- 风险事件 + 事件详情
- 审核日志

**Phase 3（高级）：**
- 场景配置
- 规则版本管理、克隆

---

## 七、待开发后端接口（REST）

需在 `kato-risk-server` 新增 Spring MVC Controller：

```java
// 规则管理
GET    /api/risk/rules          // 分页列表
POST   /api/risk/rules          // 新建规则
PUT    /api/risk/rules/{id}     // 更新规则
DELETE /api/risk/rules/{id}     // 删除规则
POST   /api/risk/rules/test     // 测试脚本

// 事件与日志
GET    /api/risk/cases           // 分页列表
GET    /api/risk/cases/{id}      // 详情
GET    /api/risk/audit-logs      // 审核日志

// 仪表盘
GET    /api/risk/stats/dashboard // 聚合数据

// 场景配置
GET    /api/risk/scenes          // 获取场景列表
PUT    /api/risk/scenes/{scene}/toggle // 启用/停用
```