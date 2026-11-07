-- ============================================================
-- 微信网页授权配置
-- ============================================================

ALTER TABLE vst_mp_config ADD COLUMN auth_mode VARCHAR(16) NOT NULL DEFAULT 'MOCK' COMMENT 'MOCK/REST';
