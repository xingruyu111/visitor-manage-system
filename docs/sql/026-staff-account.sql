-- ============================================================
-- 人员与账号改造：工号/学号关联、主角色来源、辅导员角色
-- 单校部署，school_id 保留为兼容字段
-- ============================================================

ALTER TABLE vst_staff ADD COLUMN person_no VARCHAR(64) NULL COMMENT '工号/学号' AFTER phone;
UPDATE vst_staff SET person_no = COALESCE(job_no, student_no) WHERE person_no IS NULL;
CREATE UNIQUE INDEX uk_staff_person_no ON vst_staff (person_no);

ALTER TABLE vst_user ADD COLUMN person_no VARCHAR(64) NULL COMMENT '关联人员工号/学号' AFTER phone;
UPDATE vst_user u
JOIN vst_staff s ON s.phone = u.phone
SET u.person_no = s.person_no
WHERE u.person_no IS NULL;
CREATE UNIQUE INDEX uk_user_person_no ON vst_user (person_no);

ALTER TABLE vst_user_role
  ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'MANUAL' COMMENT 'IDENTITY主角色/MANUAL附加角色';

-- 已有账号：角色与人员类型匹配的，标记为主角色
UPDATE vst_user_role r
JOIN vst_user u ON u.id = r.user_id
JOIN vst_staff s ON s.person_no = u.person_no
SET r.source = 'IDENTITY', r.is_default = 1
WHERE r.role = CASE s.person_type
    WHEN 1 THEN 5
    WHEN 2 THEN 6
    WHEN 3 THEN 7
    WHEN 4 THEN 8
    WHEN 5 THEN 5
    WHEN 6 THEN 6
    WHEN 7 THEN 9
    ELSE -1
  END;
