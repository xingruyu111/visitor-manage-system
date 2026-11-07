# backend

后端服务：Java 21 + Spring Boot 3.2.4 + Maven + MySQL + Redis。

## 目录

```
src/main/java/com/visitor/
├── VisitorManageApplication.java   # 启动类
├── common/core/Result.java         # 统一响应
└── server/
    ├── config/JacksonConfig.java   # LocalDateTime 统一格式
    └── controller/HealthController.java
src/main/resources/application.yaml # 端口 8080，context-path /api
```

## 配置

配置文件中不含任何密钥，全部通过环境变量注入：

| 变量 | 说明 | 默认 |
|---|---|---|
| `DB_HOST` / `DB_PORT` | MySQL 地址端口 | `localhost` / `3306` |
| `DB_NAME` | 数据库名 | `visitor_manage` |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号 | `root` / 空 |
| `REDIS_HOST` / `REDIS_PORT` | Redis 地址端口 | `localhost` / `6379` |
| `REDIS_USERNAME` / `REDIS_PASSWORD` | Redis ACL 账号 | `default` / 空 |

本地开发：复制根目录 `.env.example` 为 `backend/.env.local` 并填写，然后：

```powershell
powershell -ExecutionPolicy Bypass -File .\run-local.ps1
```

## 命令

```bash
mvn spring-boot:run          # 启动，http://localhost:8080/api
mvn test                     # 单元测试（不依赖外部服务）
mvn clean package -DskipTests
```

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/health` | 健康检查，同时探测 MySQL 与 Redis |
| GET | `/api/swagger-ui.html` | 接口文档 |
| GET | `/api/v3/api-docs` | OpenAPI JSON |

健康检查返回示例：

```json
{
  "code": 200,
  "data": {
    "status": "UP",
    "database": { "status": "UP", "type": "MySQL" },
    "redis": { "status": "UP", "type": "Redis" },
    "time": "2026-11-07 20:28:44"
  }
}
```

任一依赖不可用时 `status` 为 `DEGRADED`，并在对应节点返回 `error`。

## 后续

- M1：引入 MyBatis-Plus、集成适配层（`vst_integration_config` + 6 个 Port）。
