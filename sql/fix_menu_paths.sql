-- =====================================================
-- 在线更新脚本：修复菜单路径 + 补工作流子页面路由
-- 在已有 jinfu_sys 数据库上执行
-- =====================================================

USE jinfu_sys;

-- ��复工作流模块菜单路径：/flow/* → /workflow/*
UPDATE sys_menu SET path = '/workflow'        WHERE id = 19;
UPDATE sys_menu SET path = '/workflow/definition' WHERE id = 20;
UPDATE sys_menu SET component = 'workflow/definition/index' WHERE id = 20;
UPDATE sys_menu SET path = '/workflow/instance'   WHERE id = 23;
UPDATE sys_menu SET component = 'workflow/instance/index' WHERE id = 23;
UPDATE sys_menu SET path = '/workflow/task'       WHERE id = 24;
UPDATE sys_menu SET component = 'workflow/task/index' WHERE id = 24;
UPDATE sys_menu SET path = '/workflow/done'       WHERE id = 25;
UPDATE sys_menu SET component = 'workflow/done/index' WHERE id = 25;
UPDATE sys_menu SET path = '/workflow/apply'      WHERE id = 26;
UPDATE sys_menu SET component = 'workflow/apply/index' WHERE id = 26;

-- 新增：流程设计器菜单
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, path, component, perms, menu_type, icon, sort, visible, status) VALUES
(27, 19, '流程设计器', '/workflow/designer', 'workflow/designer/index', 'flow:designer:edit', 'C', 'edit', 6, 0, 0);

-- 授权
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 1, 27;

-- 新增：审批中心「新建申请」菜单
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, path, component, perms, menu_type, icon, sort, visible, status) VALUES
(47, 40, '新建申请', '/approval/start', 'approval/start/index', 'approval:start:add', 'C', 'plus-circle', 4, 0, 0);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 1, 47;

-- 审批实例表加抄送人字段
ALTER TABLE sys_process_instance ADD COLUMN IF NOT EXISTS cc_users JSON DEFAULT NULL COMMENT '抄送人信息JSON([{id,name},...])' AFTER status;

-- 验证
SELECT id, parent_id, menu_name, path, component FROM sys_menu WHERE id >= 19 AND id <= 27 ORDER BY id;
SELECT id, menu_name FROM sys_menu WHERE id >= 40 ORDER BY id;
