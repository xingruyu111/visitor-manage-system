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
| backend | Java 21、Spring Boot 3.2.4、Maven、SpringDoc、JdbcTemplate、Spring Data Redis | 8080（context-path `/api`） |
| frontend-h5 | Vue 3.5、Vant 4、Pinia、Vue Router、Axios、TypeScript | 5174 |
| frontend-web | Vue 3.5、Element Plus、ECharts、Pinia、Vue Router、Axios、TypeScript | 5173 |
| 基础设施 | MySQL 8.0（库 `visitor_manage`，utf8mb4）、Redis 7 | 3306 / 6379 |

## 环境变量

后端不在仓库里保存任何密钥，全部走环境变量（见 `.env.example`）：

| 变量 | 说明 |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | MySQL 地址、端口、库名 |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL 账号密码 |
| `REDIS_HOST` / `REDIS_PORT` | Redis 地址端口 |
| `REDIS_USERNAME` / `REDIS_PASSWORD` | Redis ACL 账号密码 |

本地开发：复制 `.env.example` 为 `backend/.env.local` 填写真实值（该文件已被 git 忽略）。

## 本地启动

### 后端

```powershell
cd backend
powershell -ExecutionPolicy Bypass -File .\run-local.ps1   # 读取 .env.local 后启动
# 或：设置好环境变量后 mvn spring-boot:run
```

启动后验证：

```bash
curl http://localhost:8080/api/health
# {"code":200,"data":{"status":"UP","database":{"status":"UP"},"redis":{"status":"UP"},...}}
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

## 当前阶段（M4）

**M0 已完成**

- 三端工程骨架；MySQL 8.0 与 Redis 7 接入，`/api/health` 同时探测两者；配置全部走环境变量；CI 工作流。

**M1 已完成**

- 表：`vst_integration_config`、`vst_integration_log`（DDL 见 `docs/sql/001-integration.sql`）。
- 六类学校接口抽象：SSO、数据中台、门闸/停车场、人脸、门户、统一待办，均为 `Port + Noop + 工厂`，未配置时自动降级。
- 配置服务：读取、保存（密钥 AES-GCM 加密、接口只回显 `******`）、连通性测试；调用统一走 `IntegrationClient` 并落日志。

**M2 已完成**

- 身份表：`vst_user`、`vst_user_role`（DDL 见 `docs/sql/002-auth.sql`）。
- 角色体系：`RoleEnum` 扩展为 9 类（超管/校管/门卫/访客/教职工/学生/商户/食堂/校友）。
- 认证：账号密码登录（BCrypt）、JWT 签发校验、登出黑名单（Redis）、`/auth/me`。
- 统一认证：`SSO` 端口接入，`protocol=MOCK` 可演练完整 SSO 流程；未配置时返回 400 并提示。
- 鉴权：`AuthInterceptor` 校验令牌、黑名单、账号状态、角色归属与学校上下文；`/web/websuper/**` 仅 SUPER、`/web/admin/**` 仅 ADMIN/SUPER；支持 `X-Current-Role` 在当前用户角色内切换。
- 登录限流：同一手机号连续失败 5 次锁定 10 分钟（Redis 计数，可通过 `LOGIN_MAX_ATTEMPTS` / `LOGIN_LOCK_MINUTES` 调整）。
- 演示账号：`13800000000 / admin123`（超管）、`13800000001 / admin123`（校管，school_id=1）——上线前必须修改。

**M3 已完成**

- 表：`vst_department`、`vst_class`、`vst_staff`（DDL 见 `docs/sql/003-org.sql`）。
- 人员类型 7 类（教职工/学生/商户/食堂/附属学校教职工/附属学校学生/校友），与角色映射。
- 部门树、人员分页/新增/修改/停用、班级维护；停用人员同时回收其账号角色。
- 数据中台同步：`protocol=MOCK` 可跑通机构/教职工/学生/校友同步，幂等 upsert，缺失人员自动停用并回收权限。
- 数据范围：ADMIN 固定本校，SUPER 必须显式传 `schoolId`。

**M4 已完成**

- 表：`vst_approval_flow`、`vst_approval_rule`、`vst_approval_instance`、`vst_approval_task`（DDL 见 `docs/sql/004-approval.sql`）。
- 流程设计器接口：保存草稿、校验、发布、停用、版本历史、预览（按被访人解析预计审批人）。
- 审批引擎：按「访客类型 + 被访人类型」匹配路由规则，未命中时回退 `VISIT_<类型>` / `VISIT_DEFAULT`。
- 审批链：普通单级（被访人）、学生两级（学生 → 辅导员）、班主任节点（附属学校学生）；实例创建时对流程定义做快照，发布新版本不影响在途审批。
- 操作：通过、驳回、批量通过、紧急审批（管理员越级）、取消；24 小时未处理由定时任务自动驳回。
- 测试：223 个用例（含 15 个 Testcontainers 用例，真实 MySQL + Redis；2C2G 机器用受限参数可跑通）。

**M6 已完成**

- 表：`vst_access_list`（人员/机动车/电动自行车黑白名单，含生效/失效时间，DDL 见 `docs/sql/006-access-list.sql`）。
- 申请时实时预检：命中黑名单直接 403 拦截；命中白名单自动通过并直接签发凭证，跳过审批。
- 名单管理接口 `/web/admin/access-list`（分页/新增/修改/删除），按学校隔离，车牌统一大写匹配。
- 定时任务每 10 分钟把过期名单置为失效（黑名单到期自动解除）。

**M7 已完成**

- 表：`vst_visit_log`（通行流水，DDL 见 `docs/sql/007-visit-log.sql`）。
- 扫码核验：按凭证 token 校验有效期/状态；`已通过 → 入校`、`已入校 → 离校`、`已离校 → 重新入校`。
- 通行次数：入校时原子扣减（`used_entries < max_entries`），次数用尽后再入校返回 400；离校不消耗次数。
- 通行记录分页 `/web/guard/verify/records`，按学校隔离。

**M8 已完成**

- 表：`vst_notification`（站内通知）、`vst_mp_config`（服务号配置）。
- 通知：提交申请时通知被访人；审批通过/驳回时通知访客；站内列表、未读数、单条/全部已读。
- 服务号：配置齐全且访客已绑定 openid 时发送模板消息，失败只记日志。
- 统一待办：推送走 `TodoMessageClient`，未配置时静默降级。

**下一阶段**：M9 前端（Web 管理台 + H5 三端）。

## 约定

- 分支：`main` 保持可运行，功能开发走 `feat/*`。
- 提交信息：`feat: `、`fix: `、`refactor: `、`chore: ` 前缀。
- 敏感配置（数据库密码、JWT 密钥、微信 appId/secret）只放环境变量，不提交仓库。
