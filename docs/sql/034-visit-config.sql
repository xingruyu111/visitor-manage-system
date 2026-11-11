SET NAMES utf8mb4;

-- ============================================================
-- 预约配置：访客类型、可预约时间段、通行次数、校友通行月份
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_visitor_type (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  code               VARCHAR(32)  NOT NULL COMMENT '类型编码',
  name               VARCHAR(64)  NOT NULL COMMENT '显示名称',
  max_entries        INT          NOT NULL DEFAULT 1 COMMENT '可通行次数，0 表示不限',
  booking_rules_json TEXT         NULL COMMENT '可预约时间段规则 JSON',
  status             TINYINT      NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  sort_no            INT          NOT NULL DEFAULT 0,
  created_at         DATETIME     NULL,
  updated_at         DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_visitor_type_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客类型配置';

CREATE TABLE IF NOT EXISTS vst_visit_config (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  school_id          BIGINT       NOT NULL,
  alumni_months_json VARCHAR(128) NULL COMMENT '校友自然月通行月份 JSON，例如 [1,2,7,8]',
  created_at         DATETIME     NULL,
  updated_at         DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_visit_config_school (school_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约全局配置';

INSERT INTO vst_visitor_type (code, name, max_entries, booking_rules_json, status, sort_no, created_at, updated_at)
VALUES
  ('NORMAL', '普通访客', 1, '[{"mode":"UNLIMITED","values":[]}]', 1, 1, NOW(), NOW()),
  ('PARENT', '学生家长', 1, '[{"mode":"UNLIMITED","values":[]}]', 1, 2, NOW(), NOW()),
  ('ALUMNI', '校友', 0, '[{"mode":"UNLIMITED","values":[]}]', 1, 3, NOW(), NOW()),
  ('CONSTRUCTION', '施工/合作', 1, '[{"mode":"UNLIMITED","values":[]}]', 1, 4, NOW(), NOW())
ON DUPLICATE KEY UPDATE code = code;

INSERT INTO vst_visit_config (school_id, alumni_months_json, created_at, updated_at)
VALUES (1, '[]', NOW(), NOW())
ON DUPLICATE KEY UPDATE school_id = school_id;
