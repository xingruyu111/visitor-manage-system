-- ============================================================
-- M2 身份与权限：用户、用户角色
-- 初始账号（仅开发/演示用，上线前必须修改密码）：
--   13800000000 / admin123  超级管理员
--   13800000001 / admin123  学校管理员（school_id=1）
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_user (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  phone           VARCHAR(20)  NOT NULL COMMENT '登录手机号',
  name            VARCHAR(64)  NULL,
  password        VARCHAR(100) NULL COMMENT 'BCrypt 散列',
  id_card         VARCHAR(32)  NULL,
  avatar_url      VARCHAR(512) NULL,
  mp_openid       VARCHAR(64)  NULL COMMENT '服务号 openid',
  union_id        VARCHAR(64)  NULL,
  status          TINYINT      NOT NULL DEFAULT 1 COMMENT '0停用 1正常',
  last_login_time DATETIME     NULL,
  last_login_ip   VARCHAR(64)  NULL,
  created_at      DATETIME     NULL,
  updated_at      DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_user_phone UNIQUE (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

CREATE TABLE IF NOT EXISTS vst_user_role (
  id         BIGINT   NOT NULL AUTO_INCREMENT,
  user_id    BIGINT   NOT NULL,
  role       TINYINT  NOT NULL COMMENT '1 SUPER/2 ADMIN/3 GUARD/4 VISITOR/5 TEACHER/6 STUDENT/7 MERCHANT/8 CANTEEN/9 ALUMNI',
  school_id  BIGINT   NULL,
  is_default TINYINT  NOT NULL DEFAULT 0,
  status     TINYINT  NOT NULL DEFAULT 1,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_user_role UNIQUE (user_id, role, school_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色';

INSERT IGNORE INTO vst_user (id, phone, name, password, status, created_at, updated_at)
VALUES (1, '13800000000', '超级管理员',
        '$2a$10$JcTcVJN9o1gGkhgCa7YWleMMWNSl3isqw1hiYwPvkVc9YUgY7r4Hu', 1, NOW(), NOW()),
       (2, '13800000001', '学校管理员',
        '$2a$10$JcTcVJN9o1gGkhgCa7YWleMMWNSl3isqw1hiYwPvkVc9YUgY7r4Hu', 1, NOW(), NOW());

INSERT IGNORE INTO vst_user_role (id, user_id, role, school_id, is_default, status, created_at, updated_at)
VALUES (1, 1, 1, NULL, 1, 1, NOW(), NOW()),
       (2, 2, 2, 1, 1, 1, NOW(), NOW());
