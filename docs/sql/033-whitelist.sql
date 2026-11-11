-- ============================================================
-- 白名单按人管理：上传人、审批人、关联预约记录、驳回原因
-- ============================================================

SET @db := DATABASE();

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'vst_access_list' AND COLUMN_NAME = 'created_by') = 0,
  'ALTER TABLE vst_access_list ADD COLUMN created_by BIGINT NULL COMMENT ''上传人'' AFTER status',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'vst_access_list' AND COLUMN_NAME = 'approved_by') = 0,
  'ALTER TABLE vst_access_list ADD COLUMN approved_by BIGINT NULL COMMENT ''审批人'' AFTER created_by',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'vst_access_list' AND COLUMN_NAME = 'approved_at') = 0,
  'ALTER TABLE vst_access_list ADD COLUMN approved_at DATETIME NULL COMMENT ''审批时间'' AFTER approved_by',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'vst_access_list' AND COLUMN_NAME = 'visitor_record_id') = 0,
  'ALTER TABLE vst_access_list ADD COLUMN visitor_record_id BIGINT NULL COMMENT ''关联预约记录'' AFTER approved_at',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'vst_access_list' AND COLUMN_NAME = 'reject_reason') = 0,
  'ALTER TABLE vst_access_list ADD COLUMN reject_reason VARCHAR(255) NULL COMMENT ''驳回原因'' AFTER visitor_record_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
