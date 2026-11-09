-- ============================================================
-- 组织树统一：部门/班级合并为一棵 vst_org，人员挂 org_id
-- 单校部署
-- ============================================================

CREATE TABLE IF NOT EXISTS vst_org (
  id              BIGINT NOT NULL AUTO_INCREMENT,
  parent_id       BIGINT NULL,
  name            VARCHAR(128) NOT NULL,
  ancestors       VARCHAR(512) NULL,
  level           INT NOT NULL DEFAULT 1,
  sort_order      INT NOT NULL DEFAULT 0,
  head_teacher_id BIGINT NULL COMMENT '班主任（班级节点）',
  external_id     VARCHAR(64) NULL COMMENT '来源编码：DEPT:id / GRADE:年级 / CLASS:id',
  status          TINYINT NOT NULL DEFAULT 1,
  created_at      DATETIME NULL,
  updated_at      DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_org_external (external_id),
  KEY idx_org_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织架构';

INSERT INTO vst_org (name, level, external_id, status, created_at, updated_at)
SELECT name, 1, CONCAT('DEPT:', id), 1, NOW(), NOW() FROM vst_department
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO vst_org (name, level, external_id, status, created_at, updated_at)
SELECT DISTINCT CONCAT(grade, '级'), 1, CONCAT('GRADE:', grade), 1, NOW(), NOW()
FROM vst_class WHERE grade IS NOT NULL AND grade <> ''
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO vst_org (name, parent_id, level, head_teacher_id, external_id, status, created_at, updated_at)
SELECT c.name,
       g.id,
       2,
       c.head_teacher_id,
       CONCAT('CLASS:', c.id),
       1, NOW(), NOW()
FROM vst_class c
LEFT JOIN vst_org g ON g.external_id = CONCAT('GRADE:', c.grade)
ON DUPLICATE KEY UPDATE name = VALUES(name), parent_id = VALUES(parent_id), head_teacher_id = VALUES(head_teacher_id);

ALTER TABLE vst_staff ADD COLUMN org_id BIGINT NULL COMMENT '组织节点' AFTER person_no;

UPDATE vst_staff s
JOIN vst_org o ON o.external_id = CONCAT('CLASS:', s.class_id)
SET s.org_id = o.id
WHERE s.class_id IS NOT NULL;

UPDATE vst_staff s
JOIN vst_org o ON o.external_id = CONCAT('DEPT:', s.dept_id)
SET s.org_id = o.id
WHERE s.org_id IS NULL AND s.dept_id IS NOT NULL;
UPDATE vst_org c
JOIN vst_class cl ON c.external_id = CONCAT('CLASS:', cl.id)
JOIN vst_org g ON g.external_id = CONCAT('GRADE:', cl.grade)
SET c.parent_id = g.id,
    c.level = 2,
    c.ancestors = CAST(g.id AS CHAR)
WHERE c.parent_id IS NULL;
WITH RECURSIVE tree AS (
  SELECT id, parent_id, CAST(id AS CHAR) AS path, 1 AS lvl
  FROM vst_department
  WHERE parent_id IS NULL
  UNION ALL
  SELECT d.id, d.parent_id, CONCAT(t.path, ',', d.id), t.lvl + 1
  FROM vst_department d
  JOIN tree t ON d.parent_id = t.id
)
UPDATE vst_org o
JOIN tree t ON o.external_id = CONCAT('DEPT:', t.id)
JOIN vst_org p ON p.external_id = CONCAT('DEPT:', t.parent_id)
SET o.parent_id = p.id,
    o.level = t.lvl,
    o.ancestors = SUBSTRING_INDEX(t.path, ',', t.lvl - 1)
WHERE t.parent_id IS NOT NULL;
