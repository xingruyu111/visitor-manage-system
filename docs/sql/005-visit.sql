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
