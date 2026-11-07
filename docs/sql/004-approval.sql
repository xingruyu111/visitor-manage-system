-- ============================================================
-- M4 审批引擎：流程定义、路由规则、实例、任务
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_approval_flow (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  flow_code       VARCHAR(64)  NOT NULL,
  name            VARCHAR(128) NOT NULL,
  version         INT          NOT NULL DEFAULT 1,
  status          TINYINT      NOT NULL DEFAULT 0 COMMENT '0草稿 1已发布 2已停用',
  definition_json TEXT         NOT NULL,
  created_by      BIGINT       NULL,
  published_at    DATETIME     NULL,
  published_by    BIGINT       NULL,
  remark          VARCHAR(255) NULL,
  created_at      DATETIME     NULL,
  updated_at      DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_flow_code_version UNIQUE (flow_code, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程定义';

CREATE TABLE IF NOT EXISTS vst_approval_rule (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  school_id    BIGINT      NULL,
  visitor_type VARCHAR(32) NULL,
  target_type  VARCHAR(32) NULL,
  flow_code    VARCHAR(64) NOT NULL,
  priority     INT         NOT NULL DEFAULT 100,
  status       TINYINT     NOT NULL DEFAULT 1,
  created_at   DATETIME    NULL,
  updated_at   DATETIME    NULL,
  PRIMARY KEY (id),
  KEY idx_rule_match (school_id, visitor_type, target_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批路由规则';

CREATE TABLE IF NOT EXISTS vst_approval_instance (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  biz_type        VARCHAR(32)  NOT NULL DEFAULT 'VISITOR_RECORD',
  biz_id          BIGINT       NOT NULL,
  school_id       BIGINT       NOT NULL,
  flow_code       VARCHAR(64)  NOT NULL,
  flow_version    INT          NOT NULL,
  definition_json TEXT         NOT NULL COMMENT '创建时快照，发布新版本不影响在途',
  status          VARCHAR(16)  NOT NULL DEFAULT 'RUNNING',
  current_seq     INT          NULL,
  applicant_id    BIGINT       NULL,
  visitor_type    VARCHAR(32)  NULL,
  target_type     VARCHAR(32)  NULL,
  target_id       BIGINT       NULL,
  started_at      DATETIME     NULL,
  ended_at        DATETIME     NULL,
  created_at      DATETIME     NULL,
  updated_at      DATETIME     NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_instance_biz UNIQUE (biz_type, biz_id),
  KEY idx_instance_school (school_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批实例';

CREATE TABLE IF NOT EXISTS vst_approval_task (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  instance_id   BIGINT       NOT NULL,
  seq           INT          NOT NULL,
  node_code     VARCHAR(64)  NOT NULL,
  node_name     VARCHAR(128) NULL,
  assignee_type VARCHAR(32)  NOT NULL,
  assignee_id   BIGINT       NULL,
  status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
  decision      VARCHAR(16)  NULL,
  comment       VARCHAR(512) NULL,
  emergency     TINYINT      NOT NULL DEFAULT 0,
  due_at        DATETIME     NULL,
  acted_at      DATETIME     NULL,
  acted_by      BIGINT       NULL,
  created_at    DATETIME     NULL,
  updated_at    DATETIME     NULL,
  PRIMARY KEY (id),
  KEY idx_task_instance (instance_id, seq),
  KEY idx_task_assignee (assignee_id, status),
  KEY idx_task_due (status, due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批任务';

-- 内置两条流程（草稿状态，需在设计器发布后生效）
INSERT IGNORE INTO vst_approval_flow (id, flow_code, name, version, status, definition_json, created_at, updated_at)
VALUES
(1, 'VISIT_DEFAULT', '默认访客流程', 1, 1,
 '{"code":"VISIT_DEFAULT","name":"默认访客流程","timeoutHours":24,"nodes":[{"seq":1,"name":"被访人审批","assignee":"TARGET"}]}',
 NOW(), NOW()),
(2, 'VISIT_STUDENT', '学生访客流程', 1, 1,
 '{"code":"VISIT_STUDENT","name":"学生访客流程","timeoutHours":24,"nodes":[{"seq":1,"name":"被访学生审批","assignee":"TARGET"},{"seq":2,"name":"辅导员审批","assignee":"COUNSELOR"}]}',
 NOW(), NOW());
