-- 角色只有分配/移除，不再使用停用状态；能否登录由账号状态控制
UPDATE vst_user_role SET status = 1;
