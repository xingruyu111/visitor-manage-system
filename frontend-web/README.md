# frontend-web

Web 管理后台：Vue 3.5 + Element Plus + Vite + TypeScript。

## 路由

| 路径 | 页面 | 说明 |
|---|---|---|
| `/login` | 登录 | 骨架阶段任意账号可进入 |
| `/dashboard` | 工作台 | 统计卡片 + 图表占位 |

## 命令

```bash
npm install
npm run dev        # http://localhost:5173
npm run build      # 类型检查 + 构建
```

开发态通过 Vite 代理把 `/api` 转发到 `http://localhost:8080`。

## 目录

```
src/
├── api/            # axios 封装
├── layouts/        # 后台布局
├── router/         # 路由 + 登录守卫
├── stores/         # Pinia
├── styles/         # 全局样式
└── views/          # login / dashboard
```

## 后续

- 角色菜单（SUPER / ADMIN）、权限守卫。
- 系统集成配置页、审批流程设计器、名单与记录页。
