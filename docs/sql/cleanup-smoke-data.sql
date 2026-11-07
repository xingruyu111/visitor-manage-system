-- 清理联调/E2E 测试数据（按测试手机号）
SET @uid1 := (SELECT id FROM vst_user WHERE phone = '13911110001' LIMIT 1);
SET @uid2 := (SELECT id FROM vst_user WHERE phone = '13911110002' LIMIT 1);

DELETE FROM vst_visit_log WHERE record_id IN (SELECT id FROM vst_visitor_record WHERE visitor_user_id IN (@uid1, @uid2));
DELETE FROM vst_alarm_record WHERE record_id IN (SELECT id FROM vst_visitor_record WHERE visitor_user_id IN (@uid1, @uid2));
DELETE FROM vst_visitor_pass WHERE record_id IN (SELECT id FROM vst_visitor_record WHERE visitor_user_id IN (@uid1, @uid2));
DELETE FROM vst_approval_task WHERE instance_id IN (SELECT id FROM vst_approval_instance WHERE biz_type = 'VISITOR_RECORD' AND biz_id IN (SELECT id FROM vst_visitor_record WHERE visitor_user_id IN (@uid1, @uid2)));
DELETE FROM vst_approval_instance WHERE biz_type = 'VISITOR_RECORD' AND biz_id IN (SELECT id FROM vst_visitor_record WHERE visitor_user_id IN (@uid1, @uid2));
DELETE FROM vst_notification WHERE user_id IN (@uid1, @uid2) OR biz_id IN (SELECT id FROM vst_visitor_record WHERE visitor_user_id IN (@uid1, @uid2));
DELETE FROM vst_visitor_record WHERE visitor_user_id IN (@uid1, @uid2);
DELETE FROM vst_user_role WHERE user_id IN (@uid1, @uid2);
DELETE FROM vst_user WHERE id IN (@uid1, @uid2);
DELETE FROM vst_staff WHERE phone IN ('13911110001', '13911110002');

SELECT
  (SELECT COUNT(*) FROM vst_user WHERE phone IN ('13911110001', '13911110002')) AS users_left,
  (SELECT COUNT(*) FROM vst_staff WHERE phone IN ('13911110001', '13911110002')) AS staff_left,
  (SELECT COUNT(*) FROM vst_visitor_record WHERE visitor_user_id IN (@uid1, @uid2)) AS records_left;
