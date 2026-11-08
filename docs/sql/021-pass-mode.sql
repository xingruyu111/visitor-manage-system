-- ============================================================
-- 通行方式：WALK 步行 / EBIKE 电动自行车 / CAR 机动车
-- ============================================================

ALTER TABLE vst_visitor_record ADD COLUMN pass_mode VARCHAR(16) NOT NULL DEFAULT 'WALK' COMMENT '通行方式';
