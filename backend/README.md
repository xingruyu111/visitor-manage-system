# backend

后端服务：Java 21 + Spring Boot 3.2.4 + Maven + MyBatis-Plus + MySQL + Redis。

## 目录

```
src/main/java/com/visitor/
├── VisitorManageApplication.java
├── common/
│   ├── core/            Result、PageResult
│   ├── exception/       BizException、GlobalExceptionHandler
│   └── util/            SecretCipher（AES-GCM）
├── domain/
│   ├── entity/          IntegrationConfig、IntegrationLog
│   ├── dto/             IntegrationConfigSaveDTO
│   └── vo/              IntegrationConfigVO
├── integration/
│   ├── IntegrationKey.java
│   ├── IntegrationFactory.java
│   ├── config/          配置服务（读写、加密、连通性测试）
│   ├── core/            IntegrationClient、日志服务、异常
│   ├── sso|sync|gate|face|portal|todo/   六类 Port + Noop
│   └── web/             配置与日志接口
└── server/
    ├── config/          Jackson、MyBatis-Plus
    ├── controller/      HealthController
    └── mapper/          IntegrationConfigMapper、IntegrationLogMapper
```

## 配置

配置文件中不含密钥，全部通过环境变量注入（见根目录 `.env.example`）：

| 变量 | 说明 | 默认 |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | MySQL 地址、端口、库名 | `localhost` / `3306` / `visitor_manage` |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL 账号 | `root` / 空 |
| `REDIS_HOST` / `REDIS_PORT` | Redis 地址端口 | `localhost` / `6379` |
| `REDIS_USERNAME` / `REDIS_PASSWORD` | Redis ACL 账号 | `default` / 空 |
| `INTEGRATION_MASTER_KEY` | 集成配置密钥的加密主密钥 | 开发默认值（仅本地） |

本地开发：复制 `.env.example` 为 `backend/.env.local` 填写，然后：

```powershell
powershell -ExecutionPolicy Bypass -File .\run-local.ps1
```

## 命令

```bash
mvn spring-boot:run          # 启动，http://localhost:8080/api
mvn test                     # 单测 + 集成测试（H2，不依赖外部服务）
mvn clean package -DskipTests
```

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/health` | 健康检查（MySQL + Redis） |
| GET | `/api/web/websuper/integration-configs` | 集成配置列表（密钥脱敏） |
| GET | `/api/web/websuper/integration-configs/{key}` | 配置详情 |
| PUT | `/api/web/websuper/integration-configs/{key}` | 保存配置（密钥留空=不修改） |
| POST | `/api/web/websuper/integration-configs/{key}/test` | 连通性测试 |
| GET | `/api/web/websuper/integration-logs` | 调用日志分页 |
| GET | `/api/swagger-ui.html` | 接口文档 |

`{key}` 取值：`SSO`、`DATA_SYNC`、`GATE`、`FACE`、`PORTAL`、`TODO_MSG`。

## 数据库

DDL 与初始化数据：`docs/sql/001-integration.sql`（幂等，可重复执行）。测试库使用 `src/test/resources/schema-h2.sql`。

## 约定

- 集成项未配置时一律返回 Noop，业务侧按降级矩阵处理，不抛异常中断主流程。
- 配置中的 `app_secret` 入库前 AES-GCM 加密，接口只回显 `******`。
- 外部调用统一走 `IntegrationClient`，自动记录 `vst_integration_log`。
