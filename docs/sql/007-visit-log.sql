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
