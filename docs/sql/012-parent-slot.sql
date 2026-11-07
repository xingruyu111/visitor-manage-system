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
