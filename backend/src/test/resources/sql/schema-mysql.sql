-- 容器测试用 MySQL 结构（= 001-integration.sql + 002-auth.sql）

-- ============================================================
-- M1 集成适配层：配置表与调用日志表（MySQL 8）
-- 执行方式：mysql -h <host> -u root -p visitor_manage < 001-integration.sql
-- 幂等：重复执行只补建缺失对象
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_integration_config (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  integration_key  VARCHAR(32)  NOT NULL COMMENT 'SSO/DATA_SYNC/GATE/FACE/PORTAL/TODO_MSG',
  protocol         VARCHAR(32)  NULL     COMMENT 'CAS/OIDC/OAUTH2/REST/SDK',
  enabled          TINYINT      NOT NULL DEFAULT 0 COMMENT '0关闭 1启用',
  base_url         VARCHAR(512) NULL,
  auth_type        VARCHAR(32)  NULL     COMMENT 'NONE/API_KEY/BASIC/OAUTH2/HMAC',
  app_id           VARCHAR(128) NULL,
  app_secret       VARCHAR(1024) NULL    COMMENT 'AES-GCM 密文，enc:v1: 前缀',
  endpoints        TEXT         NULL     COMMENT 'JSON：接口相对路径',
  field_mapping    TEXT         NULL     COMMENT 'JSON：外部字段→内部字段',
  extra            TEXT         NULL     COMMENT 'JSON：扩展参数',
  timeout_ms       INT          NOT NULL DEFAULT 5000,
  retry_times      INT          NOT NULL DEFAULT 2,
  status           TINYINT      NOT NULL DEFAULT 1,
  remark           VARCHAR(255) NULL,
  created_at       DATETIME     NULL,
  updated_at       DATETIME     NULL,
  updated_by       BIGINT       NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_integration_key UNIQUE (integration_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学校系统集成配置';

CREATE TABLE IF NOT EXISTS vst_integration_log (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  integration_key VARCHAR(32)  NOT NULL,
  action          VARCHAR(64)  NOT NULL COMMENT 'syncStaff/pushPermission/...',
  biz_type        VARCHAR(32)  NULL,
  biz_id          VARCHAR(64)  NULL,
  direction       VARCHAR(8)   NULL COMMENT 'OUT/IN',
  request_digest  VARCHAR(512) NULL COMMENT '脱敏请求摘要',
  http_status     INT          NULL,
  success         TINYINT      NOT NULL DEFAULT 0,
  error_code      VARCHAR(64)  NULL,
  error_msg       VARCHAR(512) NULL,
  cost_ms         INT          NULL,
  trace_id        VARCHAR(64)  NULL,
  created_at      DATETIME     NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学校系统集成调用日志';

-- 预置 6 个配置项，默认关闭；部署后由管理员在页面填写
INSERT IGNORE INTO vst_integration_config
  (integration_key, enabled, status, timeout_ms, retry_times, remark, created_at, updated_at)
VALUES
  ('SSO',       0, 1, 5000, 2, '统一身份认证', NOW(), NOW()),
  ('DATA_SYNC', 0, 1, 8000, 2, '数据中台',     NOW(), NOW()),
  ('GATE',      0, 1, 5000, 2, '门闸/停车场',   NOW(), NOW()),
  ('FACE',      0, 1, 5000, 2, '人脸平台',     NOW(), NOW()),
  ('PORTAL',    0, 1, 5000, 2, '融合门户/i阳光', NOW(), NOW()),
  ('TODO_MSG',  0, 1, 5000, 2, '统一待办/消息', NOW(), NOW());


-- ============================================================
-- M2 身份与权限：用户、用户角色
-- 初始账号（仅开发/演示用，上线前必须修改密码）：
--   13800000000 / admin123  超级管理员
--   13800000001 / admin123  学校管理员（school_id=1）
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_user (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  phone           VARCHAR(20)  NOT NULL COMMENT '登录手机号',
  name            VARCHAR(64)  NULL,
  password        VARCHAR(100) NULL COMMENT 'BCrypt 散列',
  id_card         VARCHAR(32)  NULL,
  avatar_url      VARCHAR(512) NULL,
  mp_openid       VARCHAR(64)  NULL COMMENT '服务号 openid',
  union_id        VARCHAR(64)  NULL,
  status          TINYINT      NOT NULL DEFAULT 1 COMMENT '0停用 1正常',
  last_login_time DATETIME     NULL,
  last_login_ip   VARCHAR(64)  NULL,
  created_at      DATETIME     NULL,
  updated_at      DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_user_phone UNIQUE (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

CREATE TABLE IF NOT EXISTS vst_user_role (
  id         BIGINT   NOT NULL AUTO_INCREMENT,
  user_id    BIGINT   NOT NULL,
  role       TINYINT  NOT NULL COMMENT '1 SUPER/2 ADMIN/3 GUARD/4 VISITOR/5 TEACHER/6 STUDENT/7 MERCHANT/8 CANTEEN/9 ALUMNI',
  school_id  BIGINT   NULL,
  is_default TINYINT  NOT NULL DEFAULT 0,
  status     TINYINT  NOT NULL DEFAULT 1,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_user_role UNIQUE (user_id, role, school_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色';

INSERT IGNORE INTO vst_user (id, phone, name, password, status, created_at, updated_at)
VALUES (1, '13800000000', '超级管理员',
        '$2a$10$JcTcVJN9o1gGkhgCa7YWleMMWNSl3isqw1hiYwPvkVc9YUgY7r4Hu', 1, NOW(), NOW()),
       (2, '13800000001', '学校管理员',
        '$2a$10$JcTcVJN9o1gGkhgCa7YWleMMWNSl3isqw1hiYwPvkVc9YUgY7r4Hu', 1, NOW(), NOW());

INSERT IGNORE INTO vst_user_role (id, user_id, role, school_id, is_default, status, created_at, updated_at)
VALUES (1, 1, 1, NULL, 1, 1, NOW(), NOW()),
       (2, 2, 2, 1, 1, 1, NOW(), NOW());
