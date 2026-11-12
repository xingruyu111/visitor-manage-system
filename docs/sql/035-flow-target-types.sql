-- ============================================================
-- 审批流程关联被访人类型（一个被访人类型只允许一条已发布流程）
-- ============================================================

SET NAMES utf8mb4;

UPDATE vst_approval_flow
SET definition_json = JSON_SET(definition_json, '$.targetTypes', JSON_ARRAY('TEACHER', 'AFFILIATED_TEACHER'))
WHERE flow_code = 'VISIT_TEACHER'
  AND JSON_EXTRACT(definition_json, '$.targetTypes') IS NULL;

UPDATE vst_approval_flow
SET definition_json = JSON_SET(definition_json, '$.targetTypes', JSON_ARRAY('STUDENT'))
WHERE flow_code = 'VISIT_STUDENT'
  AND JSON_EXTRACT(definition_json, '$.targetTypes') IS NULL;

UPDATE vst_approval_flow
SET definition_json = JSON_SET(definition_json, '$.targetTypes', JSON_ARRAY('MERCHANT'))
WHERE flow_code = 'VISIT_MERCHANT'
  AND JSON_EXTRACT(definition_json, '$.targetTypes') IS NULL;

UPDATE vst_approval_flow
SET definition_json = JSON_SET(definition_json, '$.targetTypes', JSON_ARRAY('CANTEEN'))
WHERE flow_code = 'VISIT_CANTEEN'
  AND JSON_EXTRACT(definition_json, '$.targetTypes') IS NULL;

UPDATE vst_approval_flow
SET definition_json = JSON_SET(definition_json, '$.targetTypes', JSON_ARRAY('AFFILIATED_STUDENT'))
WHERE flow_code = 'VISIT_AFFILIATED_STUDENT'
  AND JSON_EXTRACT(definition_json, '$.targetTypes') IS NULL;

UPDATE vst_approval_flow
SET definition_json = JSON_SET(definition_json, '$.targetTypes', JSON_ARRAY('ALUMNI'))
WHERE flow_code = 'VISIT_DEFAULT'
  AND JSON_EXTRACT(definition_json, '$.targetTypes') IS NULL;
