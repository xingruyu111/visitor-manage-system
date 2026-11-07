-- ============================================================
-- 门闸通行记录回传：外部记录去重
-- ============================================================

ALTER TABLE vst_visit_log ADD COLUMN external_id VARCHAR(64) NULL COMMENT '门闸记录ID';
CREATE UNIQUE INDEX uk_visit_log_external ON vst_visit_log (external_id);
