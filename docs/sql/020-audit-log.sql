-- ============================================================
-- 安全加固：操作审计日志
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_audit_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NULL,
  user_name   VARCHAR(64)  NULL,
  action      VARCHAR(64)  NOT NULL COMMENT 'VISITOR_APPLY/APPROVAL_APPROVE/...',
  biz_type    VARCHAR(32)  NULL,
  biz_id      BIGINT       NULL,
  detail      VARCHAR(512) NULL,
  ip          VARCHAR(64)  NULL,
  created_at  DATETIME     NULL,
  PRIMARY KEY (id),
  KEY idx_audit_action (action, created_at),
  KEY idx_audit_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';
