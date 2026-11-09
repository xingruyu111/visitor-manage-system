-- 人员软删除标记：删除后不在列表展示，账号停用；重新添加同 person_no 可恢复
ALTER TABLE vst_staff ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除';
