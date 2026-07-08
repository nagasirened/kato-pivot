# kato-risk-web

风控系统前端管理后台。

## 技术栈

- **框架**: React 18 + Vite
- **UI**: Ant Design 5
- **状态管理**: TanStack Query + Zustand
- **路由**: React Router v6
- **图表**: ECharts
- **语言**: TypeScript

## 页面一览

| 页面 | 路由 | 说明 |
|------|------|------|
| 仪表盘 | `/` | 概览数据：检查量、拦截率、趋势图、TOP规则 |
| 规则列表 | `/rules` | 规则管理：新建/编辑/启用禁用/删除 |
| 规则编辑 | `/rules/new`, `/rules/:id/edit` | 规则表单：SpEL表达式或Groovy脚本 |
| 脚本测试 | `/rules/test` | 在线调试Groovy脚本 |
| 风险事件 | `/cases` | 事件查询：场景/决策/用户ID筛选 |
| 事件详情 | `/cases/:id` | 单条事件决策详情 |
| 审核日志 | `/audit-logs` | 规则变更历史 |
| 场景配置 | `/scene-config` | 场景启用/停用开关 |

## 快速启动

```bash
# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build
```

## 环境变量

```env
VITE_API_BASE_URL=http://localhost:50051/api
```

## 配套后端接口

`kato-risk-server` REST API：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/risk/rules` | GET | 分页查询规则 |
| `/api/risk/rules/{id}` | GET | 规则详情 |
| `/api/risk/rules` | POST | 新建规则 |
| `/api/risk/rules/{id}` | PUT | 更新规则 |
| `/api/risk/rules/{id}` | DELETE | 删除规则 |
| `/api/risk/rules/{id}/toggle` | PUT | 启用/禁用规则 |
| `/api/risk/rules/test` | POST | 测试Groovy脚本 |
| `/api/risk/cases` | GET | 分页查询事件 |
| `/api/risk/cases/{id}` | GET | 事件详情 |
| `/api/risk/cases/{id}/handle` | PUT | 处理事件 |
| `/api/risk/audit-logs` | GET | 审核日志 |
| `/api/risk/stats/dashboard` | GET | 仪表盘数据 |
| `/api/risk/scenes` | GET | 场景配置列表 |
| `/api/risk/scenes/{scene}/toggle` | PUT | 启用/停用场景 |

## 数据库初始化

```bash
mysql -u root < kato-risk-server/src/main/resources/sql/init.sql
```

## 项目结构

```
kato-risk-web/
├── src/
│   ├── api/          # HTTP 请求封装
│   │   ├── http.ts    # Axios 实例
│   │   └── risk.ts    # 风控 API 接口
│   ├── components/    # 公共组件
│   │   └── Layout.tsx  # 侧边栏布局
│   ├── pages/         # 页面组件
│   │   ├── Dashboard/
│   │   ├── RuleList/
│   │   ├── RuleEdit/
│   │   ├── ScriptTest/
│   │   ├── CaseList/
│   │   ├── CaseDetail/
│   │   ├── AuditLog/
│   │   └── SceneConfig/
│   ├── router/        # 路由配置
│   └── App.tsx        # 应用入口
├── .env               # 环境变量
└── package.json
```