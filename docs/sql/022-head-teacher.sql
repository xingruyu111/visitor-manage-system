-- ============================================================
-- 附属学生：填写班主任工号，按班主任审批
-- ============================================================

ALTER TABLE vst_visitor_record ADD COLUMN head_teacher_no VARCHAR(32) NULL COMMENT '班主任工号';
ALTER TABLE vst_approval_instance ADD COLUMN assignee_hint BIGINT NULL COMMENT '指定审批人用户ID';

UPDATE vst_approval_flow
SET definition_json = '{"code":"VISIT_AFFILIATED_STUDENT","name":"实验学校学生访客流程","timeoutHours":24,"nodes":[{"seq":1,"name":"班主任审批","assignee":"HEAD_TEACHER"}]}',
    updated_at = NOW()
WHERE flow_code = 'VISIT_AFFILIATED_STUDENT' AND status = 1;
