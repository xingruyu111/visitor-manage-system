-- H2（MySQL 兼容模式）测试库结构：与 docs/sql/001-integration.sql 保持一致
DROP TABLE IF EXISTS vst_integration_config;
CREATE TABLE vst_integration_config (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  integration_key  VARCHAR(32)  NOT NULL,
  protocol         VARCHAR(32),
  enabled          TINYINT      NOT NULL DEFAULT 0,
  base_url         VARCHAR(512),
  auth_type        VARCHAR(32),
  app_id           VARCHAR(128),
  app_secret       VARCHAR(1024),
  endpoints        TEXT,
  field_mapping    TEXT,
  extra            TEXT,
  timeout_ms       INT          NOT NULL DEFAULT 5000,
  retry_times      INT          NOT NULL DEFAULT 2,
  status           TINYINT      NOT NULL DEFAULT 1,
  remark           VARCHAR(255),
  created_at       DATETIME,
  updated_at       DATETIME,
  updated_by       BIGINT,
  PRIMARY KEY (id),
  CONSTRAINT uk_integration_key UNIQUE (integration_key)
);

DROP TABLE IF EXISTS vst_integration_log;
CREATE TABLE vst_integration_log (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  integration_key VARCHAR(32)  NOT NULL,
  action          VARCHAR(64)  NOT NULL,
  biz_type        VARCHAR(32),
  biz_id          VARCHAR(64),
  direction       VARCHAR(8),
  request_digest  VARCHAR(512),
  http_status     INT,
  success         TINYINT      NOT NULL DEFAULT 0,
  error_code      VARCHAR(64),
  error_msg       VARCHAR(512),
  cost_ms         INT,
  trace_id        VARCHAR(64),
  created_at      DATETIME,
  PRIMARY KEY (id)
);
DROP TABLE IF EXISTS vst_user;
CREATE TABLE vst_user (
  id BIGINT NOT NULL AUTO_INCREMENT,
  phone VARCHAR(20) NOT NULL,
  name VARCHAR(64),
  password VARCHAR(100),
  id_card VARCHAR(32),
  avatar_url VARCHAR(512),
  mp_openid VARCHAR(64),
  union_id VARCHAR(64),
  status TINYINT NOT NULL DEFAULT 1,
  last_login_time DATETIME,
  last_login_ip VARCHAR(64),
  created_at DATETIME,
  updated_at DATETIME,
  PRIMARY KEY (id),
  CONSTRAINT uk_user_phone UNIQUE (phone)
);

DROP TABLE IF EXISTS vst_user_role;
CREATE TABLE vst_user_role (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role TINYINT NOT NULL,
  school_id BIGINT,
  is_default TINYINT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME,
  updated_at DATETIME,
  PRIMARY KEY (id),
  CONSTRAINT uk_user_role UNIQUE (user_id, role, school_id)
);
