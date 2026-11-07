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
