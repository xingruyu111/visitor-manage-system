-- 容器测试用 MySQL 结构（001-012）

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


-- ============================================================
-- M3 人员与组织：部门、班级、人员
-- 幂等：可重复执行
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_department (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  school_id   BIGINT       NOT NULL,
  parent_id   BIGINT       NULL,
  name        VARCHAR(128) NOT NULL,
  level       INT          NOT NULL DEFAULT 1,
  ancestors   VARCHAR(512) NULL COMMENT '祖级路径，逗号分隔',
  sort_order  INT          NOT NULL DEFAULT 0,
  external_id VARCHAR(64)  NULL COMMENT '数据中台部门编码',
  status      TINYINT      NOT NULL DEFAULT 1,
  created_at  DATETIME     NULL,
  updated_at  DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_dept_external UNIQUE (school_id, external_id),
  KEY idx_dept_school (school_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门';

CREATE TABLE IF NOT EXISTS vst_class (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  school_id       BIGINT       NOT NULL,
  name            VARCHAR(128) NOT NULL,
  grade           VARCHAR(32)  NULL,
  head_teacher_id BIGINT       NULL COMMENT '班主任',
  counselor_id    BIGINT       NULL COMMENT '辅导员',
  external_id     VARCHAR(64)  NULL COMMENT '数据中台班级编码',
  status          TINYINT      NOT NULL DEFAULT 1,
  created_at      DATETIME     NULL,
  updated_at      DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_class_external UNIQUE (school_id, external_id),
  KEY idx_class_school (school_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级';

CREATE TABLE IF NOT EXISTS vst_staff (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  school_id    BIGINT       NOT NULL,
  dept_id      BIGINT       NULL,
  class_id     BIGINT       NULL,
  name         VARCHAR(64)  NOT NULL,
  phone        VARCHAR(20)  NULL,
  person_type  TINYINT      NOT NULL DEFAULT 1 COMMENT '1教职工 2学生 3商户 4食堂 5附属学校教职工 6附属学校学生 7校友',
  job_no       VARCHAR(64)  NULL,
  student_no   VARCHAR(64)  NULL,
  position     VARCHAR(64)  NULL,
  counselor_id BIGINT       NULL,
  status       TINYINT      NOT NULL DEFAULT 1,
  sync_source  VARCHAR(32)  NOT NULL DEFAULT 'LOCAL',
  external_id  VARCHAR(64)  NULL,
  created_at   DATETIME     NULL,
  updated_at   DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_staff_external UNIQUE (school_id, external_id),
  KEY idx_staff_school (school_id),
  KEY idx_staff_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校内人员';


-- ============================================================
-- M4 审批引擎：流程定义、路由规则、实例、任务
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_approval_flow (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  flow_code       VARCHAR(64)  NOT NULL,
  name            VARCHAR(128) NOT NULL,
  version         INT          NOT NULL DEFAULT 1,
  status          TINYINT      NOT NULL DEFAULT 0 COMMENT '0草稿 1已发布 2已停用',
  definition_json TEXT         NOT NULL,
  created_by      BIGINT       NULL,
  published_at    DATETIME     NULL,
  published_by    BIGINT       NULL,
  remark          VARCHAR(255) NULL,
  created_at      DATETIME     NULL,
  updated_at      DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_flow_code_version UNIQUE (flow_code, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程定义';

CREATE TABLE IF NOT EXISTS vst_approval_rule (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  school_id    BIGINT      NULL,
  visitor_type VARCHAR(32) NULL,
  target_type  VARCHAR(32) NULL,
  flow_code    VARCHAR(64) NOT NULL,
  priority     INT         NOT NULL DEFAULT 100,
  status       TINYINT     NOT NULL DEFAULT 1,
  created_at   DATETIME    NULL,
  updated_at   DATETIME    NULL,
  PRIMARY KEY (id),
  KEY idx_rule_match (school_id, visitor_type, target_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批路由规则';

CREATE TABLE IF NOT EXISTS vst_approval_instance (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  biz_type        VARCHAR(32)  NOT NULL DEFAULT 'VISITOR_RECORD',
  biz_id          BIGINT       NOT NULL,
  school_id       BIGINT       NOT NULL,
  flow_code       VARCHAR(64)  NOT NULL,
  flow_version    INT          NOT NULL,
  definition_json TEXT         NOT NULL COMMENT '创建时快照，发布新版本不影响在途',
  status          VARCHAR(16)  NOT NULL DEFAULT 'RUNNING',
  current_seq     INT          NULL,
  applicant_id    BIGINT       NULL,
  visitor_type    VARCHAR(32)  NULL,
  target_type     VARCHAR(32)  NULL,
  target_id       BIGINT       NULL,
  started_at      DATETIME     NULL,
  ended_at        DATETIME     NULL,
  created_at      DATETIME     NULL,
  updated_at      DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_instance_biz UNIQUE (biz_type, biz_id),
  KEY idx_instance_school (school_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批实例';

CREATE TABLE IF NOT EXISTS vst_approval_task (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  instance_id   BIGINT       NOT NULL,
  seq           INT          NOT NULL,
  node_code     VARCHAR(64)  NOT NULL,
  node_name     VARCHAR(128) NULL,
  assignee_type VARCHAR(32)  NOT NULL,
  assignee_id   BIGINT       NULL,
  status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
  decision      VARCHAR(16)  NULL,
  comment       VARCHAR(512) NULL,
  emergency     TINYINT      NOT NULL DEFAULT 0,
  due_at        DATETIME     NULL,
  acted_at      DATETIME     NULL,
  acted_by      BIGINT       NULL,
  created_at    DATETIME     NULL,
  updated_at    DATETIME     NULL,
  PRIMARY KEY (id),
  KEY idx_task_instance (instance_id, seq),
  KEY idx_task_assignee (assignee_id, status),
  KEY idx_task_due (status, due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批任务';

-- 内置两条流程（草稿状态，需在设计器发布后生效）
INSERT IGNORE INTO vst_approval_flow (id, flow_code, name, version, status, definition_json, created_at, updated_at)
VALUES
(1, 'VISIT_DEFAULT', '默认访客流程', 1, 1,
 '{"code":"VISIT_DEFAULT","name":"默认访客流程","timeoutHours":24,"nodes":[{"seq":1,"name":"被访人审批","assignee":"TARGET"}]}',
 NOW(), NOW()),
(2, 'VISIT_STUDENT', '学生访客流程', 1, 1,
 '{"code":"VISIT_STUDENT","name":"学生访客流程","timeoutHours":24,"nodes":[{"seq":1,"name":"被访学生审批","assignee":"TARGET"},{"seq":2,"name":"辅导员审批","assignee":"COUNSELOR"}]}',
 NOW(), NOW());


-- ============================================================
-- M5 预约与凭证：访客记录、通行凭证
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_visitor_record (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  school_id      BIGINT       NOT NULL,
  visitor_user_id BIGINT      NOT NULL,
  visitor_name   VARCHAR(64)  NOT NULL,
  visitor_phone  VARCHAR(20)  NULL,
  id_card        VARCHAR(32)  NULL,
  visitor_type   VARCHAR(32)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/ALUMNI/PARENT/CONSTRUCTION/COOPERATION',
  target_type    VARCHAR(32)  NULL,
  target_id      BIGINT       NULL,
  target_name    VARCHAR(64)  NULL,
  visit_purpose  VARCHAR(255) NULL,
  expected_time  DATETIME     NOT NULL,
  leave_time     DATETIME     NOT NULL,
  status         TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审批 1已通过 2已拒绝 3已入校 4已离校 5已取消 6已过期',
  reject_reason  VARCHAR(255) NULL,
  created_at     DATETIME     NULL,
  updated_at     DATETIME     NULL,
  PRIMARY KEY (id),
  KEY idx_record_school (school_id, status),
  KEY idx_record_visitor (visitor_user_id),
  KEY idx_record_target (target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客预约记录';

CREATE TABLE IF NOT EXISTS vst_visitor_pass (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  school_id    BIGINT      NOT NULL,
  record_id    BIGINT      NOT NULL,
  pass_no      VARCHAR(64) NOT NULL,
  qr_token     VARCHAR(64) NOT NULL,
  valid_from   DATETIME    NOT NULL,
  valid_to     DATETIME    NOT NULL,
  max_entries  INT         NOT NULL DEFAULT 1,
  used_entries INT         NOT NULL DEFAULT 0,
  status       TINYINT     NOT NULL DEFAULT 0 COMMENT '0有效 1用尽 2过期 3撤销',
  created_at   DATETIME    NULL,
  updated_at   DATETIME    NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_pass_record UNIQUE (record_id),
  CONSTRAINT uk_pass_no UNIQUE (pass_no),
  CONSTRAINT uk_pass_qr UNIQUE (qr_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通行凭证';


-- ============================================================
-- M6 名单与车辆：人员/机动车/电动自行车黑白名单
-- list_type: 1黑名单 2白名单；target_type: PERSON/PLATE_MOTOR/PLATE_EBIKE
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_access_list (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  school_id      BIGINT       NOT NULL,
  list_type      TINYINT      NOT NULL,
  target_type    VARCHAR(16)  NOT NULL,
  target_value   VARCHAR(64)  NOT NULL COMMENT '手机号/身份证/车牌',
  person_name    VARCHAR(64)  NULL,
  plate_number   VARCHAR(32)  NULL,
  effective_from DATETIME     NULL,
  effective_to   DATETIME     NULL,
  reason         VARCHAR(255) NULL,
  source         VARCHAR(16)  NOT NULL DEFAULT 'MANUAL',
  status         TINYINT      NOT NULL DEFAULT 1 COMMENT '0已失效 1有效',
  created_at     DATETIME     NULL,
  updated_at     DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_access_list UNIQUE (school_id, list_type, target_type, target_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黑白名单';


-- ============================================================
-- M7 核验与通行：通行流水（扫码核销）
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_visit_log (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  school_id   BIGINT      NOT NULL,
  record_id   BIGINT      NOT NULL,
  pass_id     BIGINT      NULL,
  direction   VARCHAR(8)  NOT NULL COMMENT 'IN/OUT',
  source      VARCHAR(16) NOT NULL DEFAULT 'LOCAL' COMMENT 'LOCAL/GATE/FACE',
  plate_number VARCHAR(32) NULL,
  guard_id    BIGINT      NULL,
  event_time  DATETIME    NOT NULL,
  created_at  DATETIME    NULL,
  PRIMARY KEY (id),
  KEY idx_visit_log_school (school_id, event_time),
  KEY idx_visit_log_record (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通行流水';


-- ============================================================
-- M8 通知：站内通知
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_notification (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  user_id    BIGINT       NOT NULL,
  title      VARCHAR(128) NOT NULL,
  content    VARCHAR(512) NULL,
  type       VARCHAR(16)  NOT NULL DEFAULT 'SYSTEM' COMMENT 'SYSTEM/APPROVAL/NOTICE',
  biz_type   VARCHAR(32)  NULL,
  biz_id     BIGINT       NULL,
  is_read    TINYINT      NOT NULL DEFAULT 0,
  created_at DATETIME     NULL,
  PRIMARY KEY (id),
  KEY idx_notification_user (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知';


-- ============================================================
-- M8 服务号配置（模板消息）
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_mp_config (
  id                      BIGINT       NOT NULL AUTO_INCREMENT,
  app_id                  VARCHAR(64)  NOT NULL,
  secret                  VARCHAR(128) NOT NULL,
  audit_template_id       VARCHAR(64)  NULL,
  audit_result_template_id VARCHAR(64) NULL,
  created_at              DATETIME     NULL,
  updated_at              DATETIME     NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务号配置';


-- ============================================================
-- P3/P4：默认审批流程与人员类型路由 + 差异化表单字段
-- ============================================================

-- 默认流程（已存在则跳过）
INSERT IGNORE INTO vst_approval_flow (flow_code, name, version, status, definition_json, created_at, updated_at) VALUES
('VISIT_TEACHER', '教职工访客流程', 1, 1,
 '{"code":"VISIT_TEACHER","name":"教职工访客流程","timeoutHours":24,"nodes":[{"seq":1,"name":"被访人审批","assignee":"TARGET"}]}', NOW(), NOW()),
('VISIT_MERCHANT', '商户访客流程', 1, 1,
 '{"code":"VISIT_MERCHANT","name":"商户访客流程","timeoutHours":24,"nodes":[{"seq":1,"name":"被访人审批","assignee":"TARGET"},{"seq":2,"name":"保卫复核","assignee":"SECURITY"}]}', NOW(), NOW()),
('VISIT_CANTEEN', '食堂访客流程', 1, 1,
 '{"code":"VISIT_CANTEEN","name":"食堂访客流程","timeoutHours":24,"nodes":[{"seq":1,"name":"被访人审批","assignee":"TARGET"},{"seq":2,"name":"保卫复核","assignee":"SECURITY"}]}', NOW(), NOW()),
('VISIT_AFFILIATED_STUDENT', '实验学校学生访客流程', 1, 1,
 '{"code":"VISIT_AFFILIATED_STUDENT","name":"实验学校学生访客流程","timeoutHours":24,"nodes":[{"seq":1,"name":"班主任审批","assignee":"HEAD_TEACHER"},{"seq":2,"name":"辅导员审批","assignee":"COUNSELOR"}]}', NOW(), NOW());

-- 人员类型 → 流程 路由（学校/访客类型留空表示通配）
INSERT IGNORE INTO vst_approval_rule (school_id, visitor_type, target_type, flow_code, priority, status, created_at, updated_at) VALUES
(NULL, NULL, 'TEACHER', 'VISIT_TEACHER', 10, 1, NOW(), NOW()),
(NULL, NULL, 'STUDENT', 'VISIT_STUDENT', 10, 1, NOW(), NOW()),
(NULL, NULL, 'MERCHANT', 'VISIT_MERCHANT', 10, 1, NOW(), NOW()),
(NULL, NULL, 'CANTEEN', 'VISIT_CANTEEN', 10, 1, NOW(), NOW()),
(NULL, NULL, 'AFFILIATED_STUDENT', 'VISIT_AFFILIATED_STUDENT', 10, 1, NOW(), NOW()),
(NULL, NULL, 'AFFILIATED_TEACHER', 'VISIT_TEACHER', 10, 1, NOW(), NOW()),
(NULL, NULL, 'ALUMNI', 'VISIT_DEFAULT', 10, 1, NOW(), NOW());

-- 差异化表单字段
CREATE TABLE IF NOT EXISTS vst_form_field (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  visitor_type VARCHAR(32)  NOT NULL,
  field_key    VARCHAR(64)  NOT NULL,
  label        VARCHAR(64)  NOT NULL,
  field_type   VARCHAR(16)  NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT/NUMBER/DATE/SELECT/PHONE/ID_CARD',
  required     TINYINT      NOT NULL DEFAULT 0,
  options      VARCHAR(512) NULL COMMENT 'SELECT 选项，逗号分隔',
  sort_no      INT          NOT NULL DEFAULT 0,
  status       TINYINT      NOT NULL DEFAULT 1,
  created_at   DATETIME     NULL,
  updated_at   DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_form_field UNIQUE (visitor_type, field_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='差异化表单字段';

ALTER TABLE vst_visitor_record ADD COLUMN extra_json TEXT NULL COMMENT '差异化表单数据';


-- ============================================================
-- P4：部门发起批量预约（保卫处审批）流程种子
-- ============================================================

INSERT IGNORE INTO vst_approval_flow (flow_code, name, version, status, definition_json, created_at, updated_at) VALUES
('VISIT_DEPT_BATCH', '部门批量预约审批', 1, 1,
 '{"code":"VISIT_DEPT_BATCH","name":"部门批量预约审批","timeoutHours":24,"nodes":[{"seq":1,"name":"保卫处审批","assignee":"SECURITY"}]}', NOW(), NOW());


-- ============================================================
-- P4：学生家长固定时段预约
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_parent_slot (
  id             BIGINT      NOT NULL AUTO_INCREMENT,
  visitor_phone  VARCHAR(20) NOT NULL,
  weekday        TINYINT     NULL COMMENT '1-7，空表示每天',
  start_time     TIME        NOT NULL,
  end_time       TIME        NOT NULL,
  effective_from DATE        NULL,
  effective_to   DATE        NULL,
  status         TINYINT     NOT NULL DEFAULT 1,
  created_at     DATETIME    NULL,
  updated_at     DATETIME    NULL,
  PRIMARY KEY (id),
  KEY idx_parent_slot_phone (visitor_phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家长固定时段';
