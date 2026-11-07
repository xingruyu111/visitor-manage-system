# frontend-h5

移动端 H5：Vue 3.5 + Vant 4 + Vite，单应用承载三类角色。

## 路由

| 路径 | 角色 | 页面 |
|---|---|---|
| `/v/home` | 访客 | 预约入口（骨架） |
| `/a/home` | 审批人 | 待办审批（骨架） |
| `/g/home` | 保安 | 扫码核验（骨架） |

## 命令

```bash
npm install
npm run dev        # http://localhost:5174
npm run build      # 类型检查 + 构建
```

开发态通过 Vite 代理把 `/api` 转发到 `http://localhost:8080`。

## 目录

```
src/
├── api/            # axios 封装与接口模块
├── router/         # 路由（按角色分组）
├── stores/         # Pinia
├── styles/         # 全局样式
└── views/          # visitor / approver / guard
```

## 后续

- 微信 OAuth 授权与 JS-SDK 签名（需后端 `/mp/common/jsapi-ticket`）。
- 访客预约表单、审批任务、扫码核验。
