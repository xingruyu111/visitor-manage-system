# 智慧校园访客管理系统（visitor-manage-system）

高校访客预约与通行管理平台，覆盖「申请 → 审批 → 发证 → 通行 → 留痕」全链路。

## 工程结构

```
visitor-manage-system/
├── backend/          # 后端服务：Spring Boot 3.2.4 + Java 21 + Maven
├── frontend-h5/      # 移动端 H5：Vue 3 + Vant 4 + Vite（访客 / 审批人 / 保安）
├── frontend-web/     # Web 管理后台：Vue 3 + Element Plus + Vite（超管 / 校管）
├── docs/             # 设计与实施文档
└── README.md
```

## 技术栈

| 端 | 技术 | 端口 |
|---|---|---|
| backend | Java 21、Spring Boot 3.2.4、Maven、SpringDoc | 8080（context-path `/api`） |
| frontend-h5 | Vue 3.5、Vant 4、Pinia、Vue Router、Axios、TypeScript | 5174 |
| frontend-web | Vue 3.5、Element Plus、ECharts、Pinia、Vue Router、Axios、TypeScript | 5173 |

## 本地启动

### 后端

```bash
cd backend
mvn spring-boot:run           # 默认 8080，接口前缀 /api
curl http://localhost:8080/api/health
# Swagger: http://localhost:8080/api/swagger-ui.html
```

### H5

```bash
cd frontend-h5
npm install
npm run dev                   # http://localhost:5174
```

### Web 管理后台

```bash
cd frontend-web
npm install
npm run dev                   # http://localhost:5173
```

两个前端都通过 Vite 代理把 `/api` 转发到 `http://localhost:8080`，无需额外配置跨域。

## 当前阶段

- 已完成：三端工程骨架、后端健康检查、前端路由与请求层占位。
- 下一阶段：按实施方案的 M0（工程治理）→ M1（集成适配层）推进，详见
  `F:\smart-campus\docs\实施方案-模块设计与实现顺序.md`（v1.1 决策：接受 POJO 重建、只做 H5、审批流程可视化配置）。

## 约定

- 分支：`main` 保持可运行，功能开发走 `feat/*`。
- 提交信息：`feat: `、`fix: `、`refactor: `、`chore: ` 前缀。
- 敏感配置（数据库密码、JWT 密钥、微信 appId/secret）只放环境变量，不提交仓库。
