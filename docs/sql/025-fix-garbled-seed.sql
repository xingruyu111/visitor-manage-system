-- ============================================================
-- 修复历史种子数据的双重编码（UTF-8 被当作 latin1 写入）
-- 说明：仅在发现乱码时执行；执行时需使用 --default-character-set=utf8mb4
-- ============================================================

UPDATE vst_approval_flow
SET name = CONVERT(BINARY(CONVERT(name USING latin1)) USING utf8mb4),
    definition_json = CONVERT(BINARY(CONVERT(definition_json USING latin1)) USING utf8mb4)
WHERE name LIKE '%å%' OR name LIKE '%ç%';

UPDATE vst_system_setting
SET description = CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4)
WHERE description LIKE '%å%' OR description LIKE '%ç%';
