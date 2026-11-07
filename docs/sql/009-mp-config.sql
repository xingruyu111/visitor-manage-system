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
