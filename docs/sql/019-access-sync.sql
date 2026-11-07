-- ============================================================
-- 名单双向同步配置
-- ============================================================

INSERT IGNORE INTO vst_integration_config
  (integration_key, protocol, enabled, status, timeout_ms, retry_times, created_at, updated_at)
VALUES
  ('ACCESS_SYNC', 'MOCK', 1, 1, 5000, 2, NOW(), NOW());
