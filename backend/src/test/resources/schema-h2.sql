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
DROP TABLE IF EXISTS vst_department;
CREATE TABLE vst_department (
  id BIGINT NOT NULL AUTO_INCREMENT,
  school_id BIGINT NOT NULL,
  parent_id BIGINT,
  name VARCHAR(128) NOT NULL,
  level INT NOT NULL DEFAULT 1,
  ancestors VARCHAR(512),
  sort_order INT NOT NULL DEFAULT 0,
  external_id VARCHAR(64),
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME,
  updated_at DATETIME,
  PRIMARY KEY (id),
  CONSTRAINT uk_dept_external UNIQUE (school_id, external_id)
);

DROP TABLE IF EXISTS vst_class;
CREATE TABLE vst_class (
  id BIGINT NOT NULL AUTO_INCREMENT,
  school_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  grade VARCHAR(32),
  head_teacher_id BIGINT,
  counselor_id BIGINT,
  external_id VARCHAR(64),
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME,
  updated_at DATETIME,
  PRIMARY KEY (id),
  CONSTRAINT uk_class_external UNIQUE (school_id, external_id)
);

DROP TABLE IF EXISTS vst_staff;
CREATE TABLE vst_staff (
  id BIGINT NOT NULL AUTO_INCREMENT,
  school_id BIGINT NOT NULL,
  dept_id BIGINT,
  class_id BIGINT,
  name VARCHAR(64) NOT NULL,
  phone VARCHAR(20),
  person_type TINYINT NOT NULL DEFAULT 1,
  job_no VARCHAR(64),
  student_no VARCHAR(64),
  position VARCHAR(64),
  counselor_id BIGINT,
  status TINYINT NOT NULL DEFAULT 1,
  sync_source VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
  external_id VARCHAR(64),
  created_at DATETIME,
  updated_at DATETIME,
  PRIMARY KEY (id),
  CONSTRAINT uk_staff_external UNIQUE (school_id, external_id)
);
DROP TABLE IF EXISTS vst_approval_flow;
CREATE TABLE vst_approval_flow (
  id BIGINT NOT NULL AUTO_INCREMENT,
  flow_code VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  version INT NOT NULL DEFAULT 1,
  status TINYINT NOT NULL DEFAULT 0,
  definition_json TEXT NOT NULL,
  created_by BIGINT,
  published_at DATETIME,
  published_by BIGINT,
  remark VARCHAR(255),
  created_at DATETIME,
  updated_at DATETIME,
  PRIMARY KEY (id),
  CONSTRAINT uk_flow_code_version UNIQUE (flow_code, version)
);

DROP TABLE IF EXISTS vst_approval_rule;
CREATE TABLE vst_approval_rule (
  id BIGINT NOT NULL AUTO_INCREMENT,
  school_id BIGINT,
  visitor_type VARCHAR(32),
  target_type VARCHAR(32),
  flow_code VARCHAR(64) NOT NULL,
  priority INT NOT NULL DEFAULT 100,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME,
  updated_at DATETIME,
  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS vst_approval_instance;
CREATE TABLE vst_approval_instance (
  id BIGINT NOT NULL AUTO_INCREMENT,
  biz_type VARCHAR(32) NOT NULL DEFAULT 'VISITOR_RECORD',
  biz_id BIGINT NOT NULL,
  school_id BIGINT NOT NULL,
  flow_code VARCHAR(64) NOT NULL,
  flow_version INT NOT NULL,
  definition_json TEXT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'RUNNING',
  current_seq INT,
  applicant_id BIGINT,
  visitor_type VARCHAR(32),
  target_type VARCHAR(32),
  target_id BIGINT,
  started_at DATETIME,
  ended_at DATETIME,
  created_at DATETIME,
  updated_at DATETIME,
  PRIMARY KEY (id),
  CONSTRAINT uk_instance_biz UNIQUE (biz_type, biz_id)
);

DROP TABLE IF EXISTS vst_approval_task;
CREATE TABLE vst_approval_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  instance_id BIGINT NOT NULL,
  seq INT NOT NULL,
  node_code VARCHAR(64) NOT NULL,
  node_name VARCHAR(128),
  assignee_type VARCHAR(32) NOT NULL,
  assignee_id BIGINT,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  decision VARCHAR(16),
  comment VARCHAR(512),
  emergency TINYINT NOT NULL DEFAULT 0,
  due_at DATETIME,
  acted_at DATETIME,
  acted_by BIGINT,
  created_at DATETIME,
  updated_at DATETIME,
  PRIMARY KEY (id)
);
DROP TABLE IF EXISTS vst_visitor_record;
CREATE TABLE vst_visitor_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  school_id BIGINT NOT NULL,
  visitor_user_id BIGINT NOT NULL,
  visitor_name VARCHAR(64) NOT NULL,
  visitor_phone VARCHAR(20),
  id_card VARCHAR(32),
  visitor_type VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  plate_number VARCHAR(32),
  target_type VARCHAR(32),
  target_id BIGINT,
  target_name VARCHAR(64),
  visit_purpose VARCHAR(255),
  expected_time DATETIME NOT NULL,
  leave_time DATETIME NOT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  extra_json TEXT,
  reject_reason VARCHAR(255),
  created_at DATETIME,
  updated_at DATETIME,
  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS vst_visitor_pass;
CREATE TABLE vst_visitor_pass (
  id BIGINT NOT NULL AUTO_INCREMENT,
  school_id BIGINT NOT NULL,
  record_id BIGINT NOT NULL,
  pass_no VARCHAR(64) NOT NULL,
  qr_token VARCHAR(64) NOT NULL,
  valid_from DATETIME NOT NULL,
  valid_to DATETIME NOT NULL,
  max_entries INT NOT NULL DEFAULT 1,
  used_entries INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME,
  updated_at DATETIME,
  PRIMARY KEY (id),
  CONSTRAINT uk_pass_record UNIQUE (record_id),
  CONSTRAINT uk_pass_no UNIQUE (pass_no),
  CONSTRAINT uk_pass_qr UNIQUE (qr_token)
);
DROP TABLE IF EXISTS vst_access_list;
CREATE TABLE vst_access_list (
  id BIGINT NOT NULL AUTO_INCREMENT,
  school_id BIGINT NOT NULL,
  list_type TINYINT NOT NULL,
  target_type VARCHAR(16) NOT NULL,
  target_value VARCHAR(64) NOT NULL,
  person_name VARCHAR(64),
  plate_number VARCHAR(32),
  effective_from DATETIME,
  effective_to DATETIME,
  reason VARCHAR(255),
  source VARCHAR(16) NOT NULL DEFAULT 'MANUAL',
  status TINYINT NOT NULL DEFAULT 1,
  released_at DATETIME,
  release_reason VARCHAR(255),
  created_at DATETIME,
  updated_at DATETIME,
  PRIMARY KEY (id),
  CONSTRAINT uk_access_list UNIQUE (school_id, list_type, target_type, target_value)
);
DROP TABLE IF EXISTS vst_visit_log;
CREATE TABLE vst_visit_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  school_id BIGINT NOT NULL,
  record_id BIGINT NOT NULL,
  pass_id BIGINT,
  direction VARCHAR(8) NOT NULL,
  source VARCHAR(16) NOT NULL DEFAULT 'LOCAL',
  plate_number VARCHAR(32),
  guard_id BIGINT,
  external_id VARCHAR(64),
  event_time DATETIME NOT NULL,
  created_at DATETIME,
  PRIMARY KEY (id)
);
DROP TABLE IF EXISTS vst_notification;
CREATE TABLE vst_notification (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  content VARCHAR(512),
  type VARCHAR(16) NOT NULL DEFAULT 'SYSTEM',
  biz_type VARCHAR(32),
  biz_id BIGINT,
  is_read TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME,
  PRIMARY KEY (id)
);
DROP TABLE IF EXISTS vst_mp_config;
CREATE TABLE vst_mp_config (
  id BIGINT NOT NULL AUTO_INCREMENT,
  app_id VARCHAR(64) NOT NULL,
  secret VARCHAR(128) NOT NULL,
  audit_template_id VARCHAR(64),
  audit_result_template_id VARCHAR(64),
  auth_mode VARCHAR(16) NOT NULL DEFAULT 'MOCK',
  created_at DATETIME,
  updated_at DATETIME,
  PRIMARY KEY (id)
);
DROP TABLE IF EXISTS vst_form_field;
CREATE TABLE vst_form_field (
  id BIGINT NOT NULL AUTO_INCREMENT,
  visitor_type VARCHAR(32) NOT NULL,
  field_key VARCHAR(64) NOT NULL,
  label VARCHAR(64) NOT NULL,
  field_type VARCHAR(16) NOT NULL DEFAULT 'TEXT',
  required TINYINT NOT NULL DEFAULT 0,
  options VARCHAR(512),
  sort_no INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME,
  updated_at DATETIME,
  PRIMARY KEY (id),
  CONSTRAINT uk_form_field UNIQUE (visitor_type, field_key)
);
DROP TABLE IF EXISTS vst_parent_slot;
CREATE TABLE vst_parent_slot (
  id BIGINT NOT NULL AUTO_INCREMENT,
  visitor_phone VARCHAR(20) NOT NULL,
  weekday TINYINT,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  effective_from DATE,
  effective_to DATE,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME,
  updated_at DATETIME,
  PRIMARY KEY (id)
);
DROP TABLE IF EXISTS vst_notify_config;
CREATE TABLE vst_notify_config (
  id BIGINT NOT NULL AUTO_INCREMENT,
  notify_type VARCHAR(32) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  channels VARCHAR(64) NOT NULL DEFAULT 'SITE',
  updated_at DATETIME,
  PRIMARY KEY (id),
  CONSTRAINT uk_notify_type UNIQUE (notify_type)
);
DROP TABLE IF EXISTS vst_alarm_record;
CREATE TABLE vst_alarm_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  record_id BIGINT NOT NULL,
  alarm_type VARCHAR(32) NOT NULL DEFAULT 'OVERSTAY',
  message VARCHAR(255),
  handled TINYINT NOT NULL DEFAULT 0,
  handled_by BIGINT,
  handled_at DATETIME,
  created_at DATETIME,
  PRIMARY KEY (id),
  CONSTRAINT uk_alarm_record UNIQUE (record_id, alarm_type)
);