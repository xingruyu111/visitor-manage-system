-- ============================================================
-- 通行预警：超期滞留告警
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_alarm_record (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  record_id   BIGINT       NOT NULL,
  alarm_type  VARCHAR(32)  NOT NULL DEFAULT 'OVERSTAY',
  message     VARCHAR(255) NULL,
  handled     TINYINT      NOT NULL DEFAULT 0,
  handled_by  BIGINT       NULL,
  handled_at  DATETIME     NULL,
  created_at  DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_alarm_record UNIQUE (record_id, alarm_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通行预警记录';
