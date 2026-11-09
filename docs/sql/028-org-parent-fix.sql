-- 修复 027 中班级节点的父级（按班级 grade 关联年级节点）
UPDATE vst_org c
JOIN vst_class cl ON c.external_id = CONCAT('CLASS:', cl.id)
JOIN vst_org g ON g.external_id = CONCAT('GRADE:', cl.grade)
SET c.parent_id = g.id,
    c.level = 2,
    c.ancestors = CAST(g.id AS CHAR)
WHERE c.parent_id IS NULL;
