-- ============================================================
-- M6 名单与车辆：人员/机动车/电动自行车黑白名单
-- list_type: 1黑名单 2白名单；target_type: PERSON/PLATE_MOTOR/PLATE_EBIKE
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_access_list (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  school_id      BIGINT       NOT NULL,
  list_type      TINYINT      NOT NULL,
  target_type    VARCHAR(16)  NOT NULL,
  target_value   VARCHAR(64)  NOT NULL COMMENT '手机号/身份证/车牌',
  person_name    VARCHAR(64)  NULL,
  plate_number   VARCHAR(32)  NULL,
  effective_from DATETIME     NULL,
  effective_to   DATETIME     NULL,
  reason         VARCHAR(255) NULL,
  source         VARCHAR(16)  NOT NULL DEFAULT 'MANUAL',
  status         TINYINT      NOT NULL DEFAULT 1 COMMENT '0已失效 1有效',
  created_at     DATETIME     NULL,
  updated_at     DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_access_list UNIQUE (school_id, list_type, target_type, target_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黑白名单';
