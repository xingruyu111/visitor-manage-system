-- ============================================================
-- M3 人员与组织：部门、班级、人员
-- 幂等：可重复执行
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_department (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  school_id   BIGINT       NOT NULL,
  parent_id   BIGINT       NULL,
  name        VARCHAR(128) NOT NULL,
  level       INT          NOT NULL DEFAULT 1,
  ancestors   VARCHAR(512) NULL COMMENT '祖级路径，逗号分隔',
  sort_order  INT          NOT NULL DEFAULT 0,
  external_id VARCHAR(64)  NULL COMMENT '数据中台部门编码',
  status      TINYINT      NOT NULL DEFAULT 1,
  created_at  DATETIME     NULL,
  updated_at  DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_dept_external UNIQUE (school_id, external_id),
  KEY idx_dept_school (school_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门';

CREATE TABLE IF NOT EXISTS vst_class (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  school_id       BIGINT       NOT NULL,
  name            VARCHAR(128) NOT NULL,
  grade           VARCHAR(32)  NULL,
  head_teacher_id BIGINT       NULL COMMENT '班主任',
  counselor_id    BIGINT       NULL COMMENT '辅导员',
  external_id     VARCHAR(64)  NULL COMMENT '数据中台班级编码',
  status          TINYINT      NOT NULL DEFAULT 1,
  created_at      DATETIME     NULL,
  updated_at      DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_class_external UNIQUE (school_id, external_id),
  KEY idx_class_school (school_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级';

CREATE TABLE IF NOT EXISTS vst_staff (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  school_id    BIGINT       NOT NULL,
  dept_id      BIGINT       NULL,
  class_id     BIGINT       NULL,
  name         VARCHAR(64)  NOT NULL,
  phone        VARCHAR(20)  NULL,
  person_type  TINYINT      NOT NULL DEFAULT 1 COMMENT '1教职工 2学生 3商户 4食堂 5附属学校教职工 6附属学校学生 7校友',
  job_no       VARCHAR(64)  NULL,
  student_no   VARCHAR(64)  NULL,
  position     VARCHAR(64)  NULL,
  counselor_id BIGINT       NULL,
  status       TINYINT      NOT NULL DEFAULT 1,
  sync_source  VARCHAR(32)  NOT NULL DEFAULT 'LOCAL',
  external_id  VARCHAR(64)  NULL,
  created_at   DATETIME     NULL,
  updated_at   DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_staff_external UNIQUE (school_id, external_id),
  KEY idx_staff_school (school_id),
  KEY idx_staff_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校内人员';
