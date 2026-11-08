-- ============================================================
-- 系统参数配置
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_system_setting (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  setting_key   VARCHAR(64)  NOT NULL,
  setting_value VARCHAR(255) NULL,
  description   VARCHAR(255) NULL,
  updated_at    DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_setting_key UNIQUE (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数';

INSERT IGNORE INTO vst_system_setting (setting_key, setting_value, description, updated_at) VALUES
('visit.pass_modes', 'WALK,EBIKE,CAR', '允许的通行方式', NOW()),
('visit.duplicate_check_enabled', 'true', '是否识别重复预约', NOW()),
('visit.unusual_hours_enabled', 'false', '是否限制非常规时段', NOW()),
('visit.unusual_hours_start', '07:00', '允许预约开始时间', NOW()),
('visit.unusual_hours_end', '22:00', '允许预约结束时间', NOW()),
('visit.max_stay_hours', '12', '单次最长在校时长（小时）', NOW()),
('visit.expire_after_leave_minutes', '0', '离校后多少分钟置为过期', NOW()),
('security.ip_ban_max_failures', '20', 'IP 连续失败封禁阈值', NOW()),
('security.ip_ban_minutes', '15', 'IP 封禁时长（分钟）', NOW());
