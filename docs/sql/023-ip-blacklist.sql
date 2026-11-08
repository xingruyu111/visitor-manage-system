-- ============================================================
-- IP 黑名单（人工维护，支持临时/永久）
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_ip_blacklist (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  ip          VARCHAR(64)  NOT NULL,
  reason      VARCHAR(255) NULL,
  expire_at   DATETIME     NULL COMMENT 'NULL 表示永久',
  created_by  BIGINT       NULL,
  created_at  DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_ip_blacklist UNIQUE (ip)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IP 黑名单';
