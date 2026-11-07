-- ============================================================
-- 消息通知类型配置
-- notify_type: APPROVAL_RESULT / EXPIRE_REMINDER / ACCESS_ALERT / BLACKLIST_ALERT
-- channels: SITE,MP,TODO 逗号分隔
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_notify_config (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  notify_type VARCHAR(32) NOT NULL,
  enabled     TINYINT     NOT NULL DEFAULT 1,
  channels    VARCHAR(64) NOT NULL DEFAULT 'SITE',
  updated_at  DATETIME    NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_notify_type UNIQUE (notify_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知配置';

INSERT IGNORE INTO vst_notify_config (notify_type, enabled, channels, updated_at) VALUES
('APPROVAL_RESULT', 1, 'SITE', NOW()),
('EXPIRE_REMINDER', 1, 'SITE', NOW()),
('ACCESS_ALERT', 1, 'SITE', NOW()),
('BLACKLIST_ALERT', 1, 'SITE', NOW());
