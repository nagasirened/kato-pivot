# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 概述

`kato-sensitive-ui` 是 `kato-sensitive` 后端的运营管理后台。纯前端项目（Vue 3 SPA），不参与后端构建。

后端架构（管理/检测双服务、热更新、敏感词数据模型）见父仓库 `CLAUDE.md` 和 `docs/sensitive-ui-design.md`。

## Build & Dev Commands

```bash
npm install                    # 装依赖（首次或换机器）
npm run dev                    # 启动 Vite dev server，默认端口 3000
npm run build                  # 生产构建，输出到 dist/
npm run preview                # 本地预览生产包

# Node >= 18 (Vite 5 要求)。本项目没有 lint/test 脚本——不要编造。
```

## Architecture

### 整体形态

侧边栏 + 主内容区的传统后台布局。无登录鉴权（MVP），无路由守卫，无 i18n（中文硬编码）。

```
kato-sensitive-ui/
├── src/
│   ├── api/                    # 两个独立的 axios 实例
│   │   ├── manager.js          # → :8080  管理端 API
│   │   └── check.js            # → :8081  检测端 API
│   ├── views/
│   │   ├── Dashboard.vue       # 统计概览（首页）
│   │   ├── SensitiveWord/
│   │   │   ├── List.vue        # 敏感词 CRUD 列表（带分页、筛选、批量删除）
│   │   │   ├── Edit.vue        # 新增/编辑弹窗（按 data?.id 区分模式）
│   │   │   └── Import.vue      # Excel 导入导出
│   │   ├── SensitiveTest/Index.vue   # 文本检测（防抖 + 脱敏预览）
│   │   └── OperationLog/List.vue
│   ├── router/index.js         # 5 条路由 + 1 条 redirect
│   ├── stores/app.js           # Pinia（目前只有 sidebarCollapsed 占位）
│   ├── styles/dark-theme.scss  # 全局样式 + Element Plus 深色主题覆盖
│   ├── App.vue                 # 侧边栏壳
│   └── main.js                 # Vue + Pinia + ElementPlus 全局注册
├── vite.config.js              # alias @ → src/，proxy /api → :8080
└── package.json
```

### 路由表

| 路径 | 名称 | 组件 |
|------|------|------|
| `/` | — | redirect `/dashboard` |
| `/dashboard` | Dashboard | `views/Dashboard.vue` |
| `/sensitive-word/list` | SensitiveWordList | `views/SensitiveWord/List.vue` |
| `/sensitive-test` | SensitiveTest | `views/SensitiveTest/Index.vue` |
| `/import-export` | ImportExport | `views/SensitiveWord/Import.vue` |
| `/operation-log` | OperationLog | `views/OperationLog/List.vue` |

所有路由组件都用 `() => import(...)` 动态加载。

## 关键模式与约定

### 1. 双 axios 实例（架构决策）

**不要合并成一个**。管理端 `:8080` 和检测端 `:8081` 是后端独立部署的两个服务，路径前缀也不同：

- `managerApi`（`src/api/manager.js`）：CRUD、导入导出、操作日志
- `checkApi`（`src/api/check.js`）：实时文本检测

两个实例都注册了 response interceptor，把 axios 包装拆开直接返回 `response.data`，所以业务代码拿到的是后端统一响应对象（`{code, data, message}` 或裸 data），调用时不再 `.data.data`。

### 2. 敏感词数据模型

```
word: string
level: URGENT | MEDIUM | NORMAL        // 等级 → 标签颜色 danger/warning/默认
category: POLITICAL | VIOLENCE | PORN | AD | OTHER
status: ENABLE | DISABLE                // 列表里用 el-switch 切换
remark: string
```

`getLevelType()` / `getLevelText()` 这两个映射函数在 List.vue、Dashboard.vue、SensitiveTest/Index.vue 三个文件里**重复实现了三遍**——这是已知冗余，新增页面要复用时考虑抽到 `src/utils/`。

### 3. 后端响应结构

所有接口走统一分页结构：
```js
res.data = { records: [...], total: N, current, size }
```
代码里到处是 `res.data?.records || []` + `res.data?.total || 0` 的防御写法。

### 4. 弹窗复用

`SensitiveWord/Edit.vue` 同时承担"新增"和"编辑"两个职责，区别只在于 `data?.id` 是否存在：
- 父组件用 `v-model` 控制显隐，传 `:data="currentRow"`（编辑）或 `:data="null"`（新增）
- 内部 `watch(() => props.data)` 重置表单
- 成功后 `$emit('success')`，父组件刷新列表

新增类似弹窗遵循同一模式。

### 5. Excel 导入导出

- **下载**：`responseType: 'blob'`，构造 `new Blob([res], {type: '...'})`，`URL.createObjectURL` + 隐藏 `<a download>` 触发下载，用完 `revokeObjectURL`
- **上传**：`el-upload` 关掉 `auto-upload`，手动从 `file.raw` 拿 File 对象，`FormData.append('file', file)` 提交

代码见 `views/SensitiveWord/Import.vue`。

### 6. 检测结果高亮

`SensitiveTest/Index.vue` 用正则替换原文中的命中词，外面包 `<mark>` 标签。**注意 v-html 的 XSS 风险**——目前没有做 sanitize，输入文本直接拼进 HTML。如果以后允许富文本或不可信输入，需要换实现。

### 7. 检测防抖

`handleTextChange = debounce(() => { ... }, 300)`，只在文本长度 > 10 时自动触发检测。手动物理按钮 `handleCheck` 无防抖。

### 8. Vite 配置

`vite.config.js` 里：
- `@` → `src/` 别名
- `server.port = 3000`
- `server.proxy['/api']` → `http://localhost:8080`

⚠️ **只有 `/api` 走代理**：检测端 8081 没配代理，因为 `check.js` 里直接硬编码了 `baseURL: 'http://localhost:8081'`。如果以后想统一走代理，要把 8081 也加进 `vite.config.js` 并改 `check.js` 的 baseURL。

### 9. 深色主题

`styles/dark-theme.scss` 是**唯一样式入口**，所有全局变量和 Element Plus 主题覆盖都在这里。新增页面：
- 必须用 CSS 变量（`var(--bg-secondary)`、`var(--accent-blue)` 等），不要硬编码颜色
- 需要新增 Element Plus 组件主题时，在 `dark-theme.scss` 里追加 `--el-xxx` CSS 变量覆盖
- 颜色规范参考 `docs/sensitive-ui-design.md` 第 179-280 行

### 10. 状态管理

Pinia 已接入但 `stores/app.js` 目前只有一个 `sidebarCollapsed` 占位（侧边栏也没真的用它）。页面级状态全部 `ref/reactive` 在 `<script setup>` 里搞定，不要为了用 Pinia 而用 Pinia。

## 常见踩坑

- **后端 CORS**：浏览器跨域访问 8080/8081 需要后端配 CORS，否则 dev 阶段用 Vite 代理（已经配好 8080，8081 还需要自己加或等后端开 CORS）。
- **`el-tag type` 枚举**：`danger`/`warning`/`success`/`info`/`primary`，自定义值会报错。
- **`el-switch` 的 `v-model`**：必须配 `active-value`/`inactive-value`，否则绑的是 boolean 而不是字符串 'ENABLE'/'DISABLE'。
- **`el-pagination` 的 v-model 写法**：用 `v-model:current-page` / `v-model:page-size`，不是 `v-model` 一个对象。

## 已知冗余/待重构（参考用，不主动改）

- `getLevelType` / `getLevelText` 散落在三个文件里
- 列表页（List.vue / OperationLog/List.vue）的查询/分页/加载模式高度雷同，可抽 composable
- `import Result` 这类后端统一响应封装在前端没做类型化，新增 TS 改造时可一并加
- 检测结果高亮用 `v-html` 拼字符串，无 XSS 防护