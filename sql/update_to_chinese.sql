-- =====================================================
-- 数据库中文化更新脚本
-- 已有英文数据 -> 中文
-- 使用方法: 在 MySQL 客户端执行此脚本
--   mysql -u root -p jinfu_sys < update_to_chinese.sql
-- =====================================================
USE jinfu_sys;

-- =====================================================
-- 1. 部门名称中文化
-- =====================================================
UPDATE sys_dept SET dept_name = '金福集团' WHERE id = 100;
UPDATE sys_dept SET dept_name = '技术部'   WHERE id = 101;
UPDATE sys_dept SET dept_name = '人事部'   WHERE id = 102;
UPDATE sys_dept SET dept_name = '财务部'   WHERE id = 103;
UPDATE sys_dept SET dept_name = '开发组'   WHERE id = 104;
UPDATE sys_dept SET dept_name = '测试组'   WHERE id = 105;

-- =====================================================
-- 2. 管理员昵称中文化
-- =====================================================
UPDATE sys_user SET nickname = '管理员' WHERE id = 1;

-- =====================================================
-- 3. 角色名称中文化
-- =====================================================
UPDATE sys_role SET role_name = '系统管理员', remark = '拥有系统全部权限'   WHERE id = 1;
UPDATE sys_role SET role_name = '审批主管',   remark = '审批流程管理人员'   WHERE id = 2;
UPDATE sys_role SET role_name = '普通员工',   remark = '普通员工账号'       WHERE id = 3;

-- =====================================================
-- 4. 菜单名称中文化
-- =====================================================
-- Level 1: 仪表盘 / 系统管理 / 工作流 / 表单管理 / 业务流程
UPDATE sys_menu SET menu_name = '仪表盘'     WHERE id = 1;
UPDATE sys_menu SET menu_name = '系统管理'   WHERE id = 2;
UPDATE sys_menu SET menu_name = '工作流'     WHERE id = 19;
UPDATE sys_menu SET menu_name = '表单管理'   WHERE id = 27;
UPDATE sys_menu SET menu_name = '业务流程'   WHERE id = 33;

-- Level 2: 系统管理
UPDATE sys_menu SET menu_name = '用户管理' WHERE id = 3;
UPDATE sys_menu SET menu_name = '用户新增' WHERE id = 4;
UPDATE sys_menu SET menu_name = '用户修改' WHERE id = 5;
UPDATE sys_menu SET menu_name = '用户删除' WHERE id = 6;
UPDATE sys_menu SET menu_name = '角色管理' WHERE id = 7;
UPDATE sys_menu SET menu_name = '角色新增' WHERE id = 8;
UPDATE sys_menu SET menu_name = '角色修改' WHERE id = 9;
UPDATE sys_menu SET menu_name = '角色删除' WHERE id = 10;
UPDATE sys_menu SET menu_name = '菜单管理' WHERE id = 11;
UPDATE sys_menu SET menu_name = '菜单新增' WHERE id = 12;
UPDATE sys_menu SET menu_name = '菜单修改' WHERE id = 13;
UPDATE sys_menu SET menu_name = '菜单删除' WHERE id = 14;
UPDATE sys_menu SET menu_name = '部门管理' WHERE id = 15;
UPDATE sys_menu SET menu_name = '部门新增' WHERE id = 16;
UPDATE sys_menu SET menu_name = '部门修改' WHERE id = 17;
UPDATE sys_menu SET menu_name = '部门删除' WHERE id = 18;

-- Level 2: 工作流
UPDATE sys_menu SET menu_name = '流程定义' WHERE id = 20;
UPDATE sys_menu SET menu_name = '流程部署' WHERE id = 21;
UPDATE sys_menu SET menu_name = '流程删除' WHERE id = 22;
UPDATE sys_menu SET menu_name = '流程实例' WHERE id = 23;
UPDATE sys_menu SET menu_name = '待办任务' WHERE id = 24;
UPDATE sys_menu SET menu_name = '已办任务' WHERE id = 25;
UPDATE sys_menu SET menu_name = '我的申请' WHERE id = 26;

-- Level 2: 表单管理
UPDATE sys_menu SET menu_name = '表单定义'   WHERE id = 28;
UPDATE sys_menu SET menu_name = '表单新增'   WHERE id = 29;
UPDATE sys_menu SET menu_name = '表单修改'   WHERE id = 30;
UPDATE sys_menu SET menu_name = '表单删除'   WHERE id = 31;
UPDATE sys_menu SET menu_name = '表单设计器' WHERE id = 32;
UPDATE sys_menu SET menu_name = '字段权限'   WHERE id = 36;

-- Level 2: 业务流程
UPDATE sys_menu SET menu_name = '请假申请' WHERE id = 34;
UPDATE sys_menu SET menu_name = '费用报销' WHERE id = 35;
UPDATE sys_menu SET menu_name = '我的应用' WHERE id = 37;

-- =====================================================
-- 5. 表单定义中文化
-- =====================================================
UPDATE form_definition SET name = '请假申请表', description = '员工请假申请表单',
schema_json = '{"type":"object","properties":{"leave_type":{"title":"请假类型","type":"string","required":true,"enum":["sick","annual","personal","maternity"],"enumNames":["病假","年假","事假","产假"],"widget":"select"},"start_date":{"title":"开始日期","type":"string","required":true,"widget":"datePicker"},"end_date":{"title":"结束日期","type":"string","required":true,"widget":"datePicker"},"leave_days":{"title":"请假天数","type":"number","required":true,"description":"申请请假的天数"},"reason":{"title":"请假事由","type":"string","required":true,"widget":"textarea","props":{"placeholder":"请描述请假原因"}},"approved_days":{"title":"批准天数","type":"number","required":false,"description":"由审批人填写"}}}'
WHERE id = 1;

UPDATE form_definition SET name = '费用报销表', description = '员工费用报销表单',
schema_json = '{"type":"object","properties":{"expense_type":{"title":"费用类型","type":"string","required":true,"enum":["travel","meal","office","training","other"],"enumNames":["差旅费","餐饮费","办公用品","培训费","其他"],"widget":"select"},"amount":{"title":"金额（元）","type":"number","required":true,"description":"总金额（人民币）"},"expense_date":{"title":"费用日期","type":"string","required":true,"widget":"datePicker"},"description":{"title":"费用说明","type":"string","required":true,"widget":"textarea","props":{"placeholder":"请详细描述费用明细"}},"approved_amount":{"title":"批准金额","type":"number","required":false,"description":"由审批人填写"}}}'
WHERE id = 2;

-- =====================================================
-- 完成。刷新前端页面即可看到中文界面。
-- =====================================================
