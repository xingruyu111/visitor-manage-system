-- ============================================================
-- P4：部门发起批量预约（保卫处审批）流程种子
-- ============================================================

INSERT IGNORE INTO vst_approval_flow (flow_code, name, version, status, definition_json, created_at, updated_at) VALUES
('VISIT_DEPT_BATCH', '部门批量预约审批', 1, 1,
 '{"code":"VISIT_DEPT_BATCH","name":"部门批量预约审批","timeoutHours":24,"nodes":[{"seq":1,"name":"保卫处审批","assignee":"SECURITY"}]}', NOW(), NOW());
