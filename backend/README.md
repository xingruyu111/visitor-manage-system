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
├── security/
│   ├── JwtUtil.java     签发与解析
│   ├── AuthInterceptor.java
│   ├── TokenBlacklist.java
│   └── UserContext.java
└── server/
    ├── config/          Jackson、MyBatis-Plus、Security、WebMvc
    ├── controller/      Health、Auth、admin/
    ├── service/         AuthService
    └── mapper/          User、UserRole、IntegrationConfig、IntegrationLog
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
| `JWT_SECRET` | JWT 签名密钥（至少 32 字符） | 开发默认值（仅本地） |
| `JWT_EXPIRATION` | 令牌有效期（毫秒） | `86400000` |
| `LOGIN_MAX_ATTEMPTS` | 登录失败锁定阈值 | `5` |
| `LOGIN_LOCK_MINUTES` | 锁定时长（分钟） | `10` |

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

### 容器化测试（真实 MySQL + Redis）

`ContainerizedIntegrationTest` 用 Testcontainers 起 `mysql:8.0` 与 `redis:7-alpine`，覆盖 M2 鉴权与 M3 人员组织；
在没有 Docker 的环境自动跳过；有 Docker 时随 `mvn test` 一起执行。

```bash
mvn -Dtest=ContainerizedIntegrationTest test
```

> 资源要求：容器已限制内存（MySQL 384MB / Redis 96MB），配合下面的参数可在 **2C2G** 机器上跑通。
> 实测峰值约 1.5GB，未使用 swap。

2C2G 机器推荐命令（限制 Maven、测试 JVM 与容器内存，并关闭 Ryuk）：

```bash
MAVEN_OPTS="-Xmx192m -XX:MaxMetaspaceSize=96m" \
TESTCONTAINERS_RYUK_DISABLED=true \
DOCKER_API_VERSION=1.44 \
mvn -B -Dapi.version=1.44 \
    -DargLine="-Xmx384m -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC" \
    -Dtest=ContainerizedIntegrationTest test
```

若 Docker 较新（如 29.x，要求 API 版本 ≥ 1.40），`DOCKER_API_VERSION` 与 `-Dapi.version` 必须保留。

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 账号密码登录，返回 JWT |
| POST | `/api/auth/logout` | 退出登录（令牌加入黑名单） |
| GET | `/api/auth/me` | 当前登录用户与角色 |
| GET | `/api/auth/sso/authorize` | 获取统一认证跳转地址 |
| GET | `/api/auth/sso/callback` | 统一认证回调换取本地令牌 |
| GET | `/api/health` | 健康检查（MySQL + Redis） |
| GET | `/api/web/websuper/integration-configs` | 集成配置列表（仅 SUPER） |
| GET | `/api/web/websuper/integration-configs/{key}` | 配置详情 |
| PUT | `/api/web/websuper/integration-configs/{key}` | 保存配置（密钥留空=不修改） |
| POST | `/api/web/websuper/integration-configs/{key}/test` | 连通性测试 |
| GET | `/api/web/websuper/integration-logs` | 调用日志分页 |
| GET | `/api/web/admin/ping` | 学校管理员上下文（ADMIN/SUPER） |
| GET/POST | `/api/web/admin/departments` | 部门树 / 新增部门 |
| PUT/DELETE | `/api/web/admin/departments/{id}` | 修改 / 删除部门 |
| GET | `/api/web/admin/staff` | 人员分页（keyword/personType/deptId） |
| GET/POST | `/api/web/admin/staff/{id}` | 详情 / 新增 |
| PUT/DELETE | `/api/web/admin/staff/{id}` | 修改 / 停用（回收账号角色） |
| GET/POST | `/api/web/admin/classes` | 班级列表 / 新增 |
| PUT | `/api/web/admin/classes/{id}` | 修改班级 |
| POST | `/api/web/admin/org-sync/{type}` | 数据中台同步：org/staff/student/alumni |
| GET | `/api/web/websuper/approval-flows` | 审批流程列表（设计器，仅 SUPER） |
| GET | `/api/web/websuper/approval-flows/{code}` | 流程详情（可指定 version） |
| PUT | `/api/web/websuper/approval-flows/{code}/draft` | 保存草稿 |
| POST | `/api/web/websuper/approval-flows/{code}/validate` | 校验 |
| POST | `/api/web/websuper/approval-flows/{code}/publish` | 发布 |
| POST | `/api/web/websuper/approval-flows/{code}/offline` | 停用 |
| GET | `/api/web/websuper/approval-flows/{code}/versions` | 版本历史 |
| POST | `/api/web/websuper/approval-flows/{code}/preview` | 预览节点与预计审批人 |
| GET | `/api/web/approval/tasks` | 我的待审批任务 |
| POST | `/api/web/approval/tasks/{id}/approve` | 通过（`emergency=true` 为紧急审批） |
| POST | `/api/web/approval/tasks/{id}/reject` | 驳回 |
| POST | `/api/web/approval/tasks/batch-approve` | 批量通过 |
| GET | `/api/web/approval/instances` | 审批实例分页 |
| POST | `/api/web/approval/instances/{id}/cancel` | 取消审批 |
| GET | `/api/swagger-ui.html` | 接口文档 |

> `/web/admin/**` 需要 ADMIN 或 SUPER；ADMIN 的学校取登录上下文，SUPER 需通过 `schoolId` 参数指定。

鉴权头：

```
Authorization: Bearer <jwt>
X-Current-Role: 2          # 可选，在用户已有角色间切换
```

角色编码：1 SUPER、2 ADMIN、3 GUARD、4 VISITOR、5 TEACHER、6 STUDENT、7 MERCHANT、8 CANTEEN、9 ALUMNI。

演示账号（`docs/sql/002-auth.sql`）：`13800000000 / admin123`（SUPER）、`13800000001 / admin123`（ADMIN，school_id=1），上线前必须修改。

`{key}` 取值：`SSO`、`DATA_SYNC`、`GATE`、`FACE`、`PORTAL`、`TODO_MSG`。

## 数据库

DDL 与初始化数据：`docs/sql/001-integration.sql`（幂等，可重复执行）。测试库使用 `src/test/resources/schema-h2.sql`。

## 约定

- 集成项未配置时一律返回 Noop，业务侧按降级矩阵处理，不抛异常中断主流程。
- 配置中的 `app_secret` 入库前 AES-GCM 加密，接口只回显 `******`。
- 外部调用统一走 `IntegrationClient`，自动记录 `vst_integration_log`。
