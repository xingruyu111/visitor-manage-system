-- 辅导员与组织关联：一个组织只能配置一个辅导员，负责该组织及所有下级
CREATE TABLE IF NOT EXISTS vst_counselor_org (
  id                 BIGINT NOT NULL AUTO_INCREMENT,
  school_id          BIGINT NOT NULL DEFAULT 1,
  counselor_staff_id BIGINT NOT NULL COMMENT '辅导员：vst_staff.id',
  org_id             BIGINT NOT NULL COMMENT '负责组织：vst_org.id，含所有下级',
  created_by         BIGINT,
  created_at         DATETIME,
  updated_at         DATETIME,
  PRIMARY KEY (id),
  UNIQUE KEY uk_counselor_org (school_id, org_id),
  KEY idx_counselor_staff (counselor_staff_id),
  KEY idx_counselor_school (school_id)
) COMMENT '辅导员与组织关联';

-- 附属学生按文档：访客填写班主任工号，由工号对应教师单级审批，不再走辅导员节点
UPDATE vst_approval_flow
SET definition_json = '{"code":"VISIT_AFFILIATED_STUDENT","name":"实验学校学生访客流程","timeoutHours":24,"nodes":[{"seq":1,"name":"工号对应教师审批","assignee":"HEAD_TEACHER"}]}',
    version = version + 1,
    updated_at = NOW()
WHERE flow_code = 'VISIT_AFFILIATED_STUDENT';
