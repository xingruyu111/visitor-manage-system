-- ============================================================
-- P3/P4：默认审批流程与人员类型路由 + 差异化表单字段
-- ============================================================

-- 默认流程（已存在则跳过）
INSERT IGNORE INTO vst_approval_flow (flow_code, name, version, status, definition_json, created_at, updated_at) VALUES
('VISIT_TEACHER', '教职工访客流程', 1, 1,
 '{"code":"VISIT_TEACHER","name":"教职工访客流程","timeoutHours":24,"nodes":[{"seq":1,"name":"被访人审批","assignee":"TARGET"}]}', NOW(), NOW()),
('VISIT_MERCHANT', '商户访客流程', 1, 1,
 '{"code":"VISIT_MERCHANT","name":"商户访客流程","timeoutHours":24,"nodes":[{"seq":1,"name":"被访人审批","assignee":"TARGET"}]}', NOW(), NOW()),
('VISIT_CANTEEN', '食堂访客流程', 1, 1,
 '{"code":"VISIT_CANTEEN","name":"食堂访客流程","timeoutHours":24,"nodes":[{"seq":1,"name":"被访人审批","assignee":"TARGET"}]}', NOW(), NOW()),
('VISIT_AFFILIATED_STUDENT', '实验学校学生访客流程', 1, 1,
 '{"code":"VISIT_AFFILIATED_STUDENT","name":"实验学校学生访客流程","timeoutHours":24,"nodes":[{"seq":1,"name":"班主任审批","assignee":"HEAD_TEACHER"}]}', NOW(), NOW());

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
