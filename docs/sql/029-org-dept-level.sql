-- 修复 027 中部门节点的层级（按 vst_department.parent_id 还原）
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
