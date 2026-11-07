-- ============================================================
-- P5：名单人工提前解除
-- ============================================================

ALTER TABLE vst_access_list ADD COLUMN released_at DATETIME NULL COMMENT '解除时间';
ALTER TABLE vst_access_list ADD COLUMN release_reason VARCHAR(255) NULL COMMENT '解除原因';
