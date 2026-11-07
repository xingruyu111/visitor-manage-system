# backend

后端服务：Java 21 + Spring Boot 3.2.4 + Maven。

## 目录

```
src/main/java/com/visitor/
├── VisitorManageApplication.java   # 启动类
├── common/core/Result.java         # 统一响应
└── server/controller/              # 接口层（当前仅 HealthController）
src/main/resources/application.yaml # 端口 8080，context-path /api
```

## 命令

```bash
mvn spring-boot:run          # 启动，http://localhost:8080/api
mvn test                     # 单元测试
mvn clean package -DskipTests
```

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/health` | 健康检查 |
| GET | `/api/swagger-ui.html` | 接口文档 |
| GET | `/api/v3/api-docs` | OpenAPI JSON |

## 后续

- M0：代码规范、密钥外置、CI。
- M1：引入 MyBatis-Plus、MySQL、Redis，搭集成适配层（见实施方案 3.x）。
