-- =====================================================
-- JinFu OA System - Database Init Script
-- Database: MySQL 8.4
-- Flowable tables (ACT_*) are auto-created by the engine
-- on first startup (flowable.database-schema-update=true)
-- =====================================================

DROP DATABASE IF EXISTS jinfu_sys;
CREATE DATABASE jinfu_sys DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE jinfu_sys;

-- =====================================================
-- 1. sys_dept (Department)
-- =====================================================
CREATE TABLE sys_dept (
    id          BIGINT       NOT NULL COMMENT 'Primary Key',
    parent_id   BIGINT       DEFAULT 0  COMMENT 'Parent Dept ID',
    dept_name   VARCHAR(50)  NOT NULL   COMMENT 'Dept Name',
    sort        INT          DEFAULT 0  COMMENT 'Display Order',
    leader      VARCHAR(20)  DEFAULT NULL COMMENT 'Leader',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT 'Phone',
    email       VARCHAR(50)  DEFAULT NULL COMMENT 'Email',
    status      TINYINT      DEFAULT 0  COMMENT '0=Active 1=Disabled',
    create_by   BIGINT       DEFAULT NULL,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT       DEFAULT NULL,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      DEFAULT 0  COMMENT '0=Exists 1=Deleted',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Department Table';

-- =====================================================
-- 2. sys_user (User)
-- =====================================================
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL COMMENT 'Primary Key',
    dept_id     BIGINT       DEFAULT NULL COMMENT 'Dept ID',
    username    VARCHAR(50)  NOT NULL   COMMENT 'Login Username',
    password    VARCHAR(100) DEFAULT NULL COMMENT 'BCrypt Password',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT 'Display Name',
    avatar      VARCHAR(255) DEFAULT NULL COMMENT 'Avatar URL',
    email       VARCHAR(50)  DEFAULT NULL COMMENT 'Email',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT 'Phone',
    status      TINYINT      DEFAULT 0  COMMENT '0=Active 1=Disabled',
    create_by   BIGINT       DEFAULT NULL,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT       DEFAULT NULL,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      DEFAULT 0  COMMENT '0=Exists 1=Deleted',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User Table';

-- =====================================================
-- 3. sys_role (Role)
-- =====================================================
CREATE TABLE sys_role (
    id          BIGINT       NOT NULL COMMENT 'Primary Key',
    role_name   VARCHAR(30)  NOT NULL   COMMENT 'Role Name',
    role_key    VARCHAR(100) NOT NULL   COMMENT 'Role Key (e.g. admin)',
    sort        INT          DEFAULT 0  COMMENT 'Display Order',
    status      TINYINT      DEFAULT 0  COMMENT '0=Active 1=Disabled',
    remark      VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
    create_by   BIGINT       DEFAULT NULL,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT       DEFAULT NULL,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      DEFAULT 0  COMMENT '0=Exists 1=Deleted',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_key (role_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role Table';

-- =====================================================
-- 4. sys_menu (Menu / Permission)
-- =====================================================
CREATE TABLE sys_menu (
    id          BIGINT       NOT NULL COMMENT 'Primary Key',
    parent_id   BIGINT       DEFAULT 0  COMMENT 'Parent Menu ID',
    menu_name   VARCHAR(50)  NOT NULL   COMMENT 'Menu Name',
    path        VARCHAR(200) DEFAULT '' COMMENT 'Route Path',
    component   VARCHAR(255) DEFAULT NULL COMMENT 'Component Path',
    perms       VARCHAR(100) DEFAULT NULL COMMENT 'Permission Code (e.g. system:user:add)',
    menu_type   CHAR(1)      DEFAULT '' COMMENT 'M=Directory C=Menu F=Button',
    icon        VARCHAR(100) DEFAULT '#' COMMENT 'Icon',
    sort        INT          DEFAULT 0  COMMENT 'Display Order',
    visible     TINYINT      DEFAULT 0  COMMENT '0=Visible 1=Hidden',
    status      TINYINT      DEFAULT 0  COMMENT '0=Active 1=Disabled',
    create_by   BIGINT       DEFAULT NULL,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT       DEFAULT NULL,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      DEFAULT 0  COMMENT '0=Exists 1=Deleted',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Menu/Permission Table';

-- =====================================================
-- 5. sys_user_role (User-Role Relation)
-- =====================================================
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL COMMENT 'User ID',
    role_id BIGINT NOT NULL COMMENT 'Role ID',
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User-Role Relation';

-- =====================================================
-- 6. sys_role_menu (Role-Menu Relation)
-- =====================================================
CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL COMMENT 'Role ID',
    menu_id BIGINT NOT NULL COMMENT 'Menu ID',
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role-Menu Relation';

-- =====================================================
-- 7. form_definition (Dynamic Form Definition)
-- =====================================================
CREATE TABLE form_definition (
    id          BIGINT       NOT NULL COMMENT 'Primary Key',
    form_key    VARCHAR(100) NOT NULL   COMMENT 'Unique business key (e.g. leave_form)',
    name        VARCHAR(100) NOT NULL   COMMENT 'Form Name',
    description VARCHAR(500) DEFAULT NULL COMMENT 'Description',
    schema_json LONGTEXT     COMMENT 'JSON Schema (field definitions)',
    version     INT          DEFAULT 1  COMMENT 'Version Number',
    status      TINYINT      DEFAULT 0  COMMENT '0=Draft 1=Published 2=Deprecated',
    create_by   BIGINT       DEFAULT NULL,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT       DEFAULT NULL,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      DEFAULT 0  COMMENT '0=Exists 1=Deleted',
    PRIMARY KEY (id),
    UNIQUE KEY uk_form_key (form_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Form Definition Table';

-- =====================================================
-- 8. form_field_permission (Field Permission per BPMN Node)
-- =====================================================
CREATE TABLE form_field_permission (
    id          BIGINT       NOT NULL COMMENT 'Primary Key',
    form_id     BIGINT       NOT NULL   COMMENT 'Form Definition ID',
    proc_def_id VARCHAR(64)  DEFAULT NULL COMMENT 'Flowable Process Definition ID',
    node_id     VARCHAR(64)  NOT NULL   COMMENT 'BPMN Node ID (e.g. activity_manager_approve)',
    field_key   VARCHAR(100) NOT NULL   COMMENT 'Field business key (e.g. leave_days)',
    permission  VARCHAR(20)  NOT NULL DEFAULT 'readonly' COMMENT 'edit / readonly / required / hidden',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_form_node (form_id, node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Form Field Permission Table';

-- =====================================================
-- 9. form_instance (Form Instance - Business Data)
-- =====================================================
CREATE TABLE form_instance (
    id                  BIGINT       NOT NULL COMMENT 'Primary Key',
    form_key            VARCHAR(100) NOT NULL   COMMENT 'Form Definition Key',
    proc_inst_id        VARCHAR(64)  DEFAULT NULL COMMENT 'Flowable Process Instance ID',
    business_data_json  LONGTEXT     COMMENT 'Business data (JSON)',
    title               VARCHAR(200) DEFAULT NULL COMMENT 'Instance Title',
    creator             BIGINT       DEFAULT NULL COMMENT 'Creator User ID',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_proc_inst (proc_inst_id),
    KEY idx_form_key (form_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Form Instance Table';

-- =====================================================
-- Initial Data: Departments
-- =====================================================
INSERT INTO sys_dept (id, parent_id, dept_name, sort, leader, status) VALUES
(100, 0,  '金福集团', 0, 'CEO', 0),
(101, 100,'技术部',   1, 'CTO', 0),
(102, 100,'人事部',   2, '人事总监', 0),
(103, 100,'财务部',   3, 'CFO', 0),
(104, 101,'开发组',   1, '开发组长', 0),
(105, 101,'测试组',   2, '测试组长', 0);

-- =====================================================
-- Initial Data: Admin User (password = admin123, BCrypt encoded)
-- =====================================================
INSERT INTO sys_user (id, dept_id, username, password, nickname, email, phone, status) VALUES
(1, 100, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '管理员', 'admin@jinfu.com', '13800000000', 0);

-- =====================================================
-- Initial Data: Roles
-- =====================================================
INSERT INTO sys_role (id, role_name, role_key, sort, status, remark) VALUES
(1, '系统管理员',    'admin',  1, 0, '拥有系统全部权限'),
(2, '审批主管',      'manager',2, 0, '审批流程管理人员'),
(3, '普通员工',      'employee',3,0, '普通员工账号');

-- =====================================================
-- Initial Data: User-Role Relation
-- =====================================================
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1);

-- =====================================================
-- Initial Data: Menus
-- =====================================================
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, perms, menu_type, icon, sort, visible, status) VALUES
-- Level 1: 仪表盘
(1, 0, '仪表盘',     '/dashboard',  'dashboard/index',   NULL,             'M', 'dashboard',  1, 0, 0),
-- Level 1: 系统管理
(2, 0, '系统管理',   '/system',     NULL,                 NULL,             'M', 'setting',    2, 0, 0),
-- Level 2: 系统管理 > 用户管理
(3, 2, '用户管理',   '/system/user', 'system/user/index', 'system:user:list','C', 'user',      1, 0, 0),
(4, 3, '用户新增',   '',             NULL,                 'system:user:add', 'F', '#',         1, 0, 0),
(5, 3, '用户修改',   '',             NULL,                 'system:user:edit','F', '#',         2, 0, 0),
(6, 3, '用户删除',   '',             NULL,                 'system:user:del', 'F', '#',         3, 0, 0),
-- Level 2: 系统管理 > 角色管理
(7, 2, '角色管理',   '/system/role', 'system/role/index', 'system:role:list','C', 'peoples',   2, 0, 0),
(8, 7, '角色新增',   '',             NULL,                 'system:role:add', 'F', '#',         1, 0, 0),
(9, 7, '角色修改',   '',             NULL,                 'system:role:edit','F', '#',         2, 0, 0),
(10,7, '角色删除',   '',             NULL,                 'system:role:del', 'F', '#',         3, 0, 0),
-- Level 2: 系统管理 > 菜单管理
(11,2, '菜单管理',   '/system/menu', 'system/menu/index', 'system:menu:list','C', 'tree-table',3, 0, 0),
(12,11,'菜单新增',   '',             NULL,                 'system:menu:add', 'F', '#',         1, 0, 0),
(13,11,'菜单修改',   '',             NULL,                 'system:menu:edit','F', '#',         2, 0, 0),
(14,11,'菜单删除',   '',             NULL,                 'system:menu:del', 'F', '#',         3, 0, 0),
-- Level 2: 系统管理 > 部门管理
(15,2, '部门管理',   '/system/dept', 'system/dept/index', 'system:dept:list','C', 'tree',      4, 0, 0),
(16,15,'部门新增',   '',             NULL,                 'system:dept:add', 'F', '#',         1, 0, 0),
(17,15,'部门修改',   '',             NULL,                 'system:dept:edit','F', '#',         2, 0, 0),
(18,15,'部门删除',   '',             NULL,                 'system:dept:del', 'F', '#',         3, 0, 0),
-- Level 1: 工作流
(19,0, '工作流',     '/workflow',   NULL,                 NULL,             'M', 'workflow',  3, 0, 0),
-- Level 2: 工作流 > 流程定义
(20,19,'流程定义',   '/workflow/definition', 'workflow/definition/index', 'flow:definition:list', 'C', 'deploy', 1, 0, 0),
(21,20,'流程部署',   '',                 NULL, 'flow:definition:deploy','F', '#', 1, 0, 0),
(22,20,'流程删除',   '',                 NULL, 'flow:definition:del',   'F', '#', 2, 0, 0),
(27,19,'流程设计器', '/workflow/designer', 'workflow/designer/index', 'flow:designer:edit',    'C', 'edit',     6, 0, 0),
-- Level 2: 工作流 > 流程实例
(23,19,'流程实例',   '/workflow/instance',   'workflow/instance/index',   'flow:instance:list',   'C', 'example',  2, 0, 0),
-- Level 2: 工作流 > 待办任务
(24,19,'待办任务',   '/workflow/task',      'workflow/task/index',      'flow:task:todo',       'C', 'todo',     3, 0, 0),
(25,19,'已办任务',   '/workflow/done',      'workflow/done/index',      'flow:task:done',       'C', 'finished', 4, 0, 0),
(26,19,'我的申请',   '/workflow/apply',     'workflow/apply/index',     'flow:task:apply',      'C', 'edit',     5, 0, 0),
-- Level 1: 表单管理
(27,0, '表单管理',   '/form',             NULL,                      NULL,                   'M', 'form',     4, 0, 0),
(28,27,'表单定义',   '/form/definition',  'form/definition/index',  'form:definition:list', 'C', 'table',    1, 0, 0),
(29,28,'表单新增',   '',                  NULL,                      'form:definition:add', 'F', '#',         1, 0, 0),
(30,28,'表单修改',   '',                  NULL,                      'form:definition:edit','F', '#',         2, 0, 0),
(31,28,'表单删除',   '',                  NULL,                      'form:definition:del', 'F', '#',         3, 0, 0),
(32,27,'表单设计器', '/form/designer',  'form/designer/index',    'form:designer:edit',  'C', 'build',    2, 0, 0),
(36,27,'字段权限',   '/form/permission','form/permission/index','form:permission:edit','C', 'safety', 3, 0, 0),
-- Level 1: 业务流程
(33,0, '业务流程',   '/business',         NULL,                      NULL,                  'M', 'business', 5, 0, 0),
(34,33,'请假申请',   '/business/leave/apply',   'business/leave/index',   'business:leave:apply','C', 'leave',    1, 0, 0),
(35,33,'费用报销',   '/business/expense/apply', 'business/expense/index', 'business:expense:apply','C', 'money',  2, 0, 0),
(37,33,'我的应用',   '/business/my-applications','business/my-applications/index','business:apply:list','C','list',3,0,0);

-- =====================================================
-- Initial Data: Role-Menu (Admin gets all menus)
-- =====================================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- =====================================================
-- Initial Data: Form Definition (Leave Form)
-- =====================================================
INSERT INTO form_definition (id, form_key, name, description, schema_json, version, status) VALUES
(1, 'leave_form', '请假申请表', '员工请假申请表单',
'{"type":"object","properties":{"leave_type":{"title":"请假类型","type":"string","required":true,"enum":["sick","annual","personal","maternity"],"enumNames":["病假","年假","事假","产假"],"widget":"select"},"start_date":{"title":"开始日期","type":"string","required":true,"widget":"datePicker"},"end_date":{"title":"结束日期","type":"string","required":true,"widget":"datePicker"},"leave_days":{"title":"请假天数","type":"number","required":true,"description":"申请请假的天数"},"reason":{"title":"请假事由","type":"string","required":true,"widget":"textarea","props":{"placeholder":"请描述请假原因"}},"approved_days":{"title":"批准天数","type":"number","required":false,"description":"由审批人填写"}}}',
1, 1),

(2, 'expense_form', '费用报销表', '员工费用报销表单',
'{"type":"object","properties":{"expense_type":{"title":"费用类型","type":"string","required":true,"enum":["travel","meal","office","training","other"],"enumNames":["差旅费","餐饮费","办公用品","培训费","其他"],"widget":"select"},"amount":{"title":"金额（元）","type":"number","required":true,"description":"总金额（人民币）"},"expense_date":{"title":"费用日期","type":"string","required":true,"widget":"datePicker"},"description":{"title":"费用说明","type":"string","required":true,"widget":"textarea","props":{"placeholder":"请详细描述费用明细"}},"approved_amount":{"title":"批准金额","type":"number","required":false,"description":"由审批人填写"}}}',
1, 1);

-- =====================================================
-- 10. sys_process_template (Custom Approval Process Template)
-- =====================================================
CREATE TABLE sys_process_template (
    id              BIGINT       NOT NULL COMMENT '主键',
    template_name   VARCHAR(100) NOT NULL   COMMENT '模板名称',
    description     VARCHAR(500) DEFAULT NULL COMMENT '描述',
    dept_id         BIGINT       DEFAULT NULL COMMENT '所属部门ID(NULL=全公司可用)',
    form_id         BIGINT       NOT NULL   COMMENT '关联表单定义ID',
    step_chain      JSON         NOT NULL   COMMENT '审批步骤链JSON',
    status          TINYINT      DEFAULT 0  COMMENT '0=启用 1=停用',
    create_by       BIGINT       DEFAULT NULL,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT       DEFAULT NULL,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      DEFAULT 0  COMMENT '0=存在 1=删除',
    PRIMARY KEY (id),
    KEY idx_dept_id (dept_id),
    KEY idx_form_id (form_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程模板表';

-- =====================================================
-- 11. sys_process_instance (Approval Process Instance)
-- =====================================================
CREATE TABLE sys_process_instance (
    id                  BIGINT       NOT NULL COMMENT '主键',
    template_id         BIGINT       NOT NULL   COMMENT '模板ID',
    template_name       VARCHAR(100) NOT NULL   COMMENT '模板名称快照',
    form_id             BIGINT       NOT NULL   COMMENT '表单定义ID',
    form_schema_snapshot JSON        NOT NULL   COMMENT '表单Schema快照',
    form_data           JSON         DEFAULT NULL COMMENT '用户填写的表单数据',
    title               VARCHAR(200) NOT NULL   COMMENT '审批标题',
    initiator_id        BIGINT       NOT NULL   COMMENT '发起人ID',
    initiator_name      VARCHAR(50)  NOT NULL   COMMENT '发起人姓名',
    dept_id             BIGINT       DEFAULT NULL COMMENT '发起部门ID',
    current_step        INT          DEFAULT 1  COMMENT '当前步骤序号(从1开始)',
    total_steps         INT          NOT NULL   COMMENT '总步骤数',
    step_chain_snapshot JSON        NOT NULL   COMMENT '步骤链快照',
    status              VARCHAR(20)  DEFAULT 'pending' COMMENT 'pending=审批中 approved=已通过 rejected=已驳回 cancelled=已撤销',
    cc_users            JSON         DEFAULT NULL COMMENT '抄送人信息JSON([{id,name},...])',
    create_by           BIGINT       DEFAULT NULL,
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT       DEFAULT NULL,
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT      DEFAULT 0  COMMENT '0=存在 1=删除',
    PRIMARY KEY (id),
    KEY idx_template_id (template_id),
    KEY idx_initiator_id (initiator_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程实例表';

-- =====================================================
-- 12. sys_approval_node (Approval Record per Node)
-- =====================================================
CREATE TABLE sys_approval_node (
    id              BIGINT       NOT NULL COMMENT '主键',
    instance_id     BIGINT       NOT NULL   COMMENT '审批实例ID',
    step_order      INT          NOT NULL   COMMENT '步骤序号',
    step_name       VARCHAR(100) NOT NULL   COMMENT '步骤名称',
    approver_type   VARCHAR(20)  NOT NULL   COMMENT '审批人类型: specific_user=指定用户 role=按角色 dept_leader=部门负责人',
    approver_value  VARCHAR(100) NOT NULL   COMMENT '审批人值: userId/roleKey/deptId',
    approver_id     BIGINT       DEFAULT NULL COMMENT '实际审批人ID',
    approver_name   VARCHAR(50)  DEFAULT NULL COMMENT '实际审批人姓名',
    action          VARCHAR(20)  DEFAULT 'pending' COMMENT 'pending=待审批 approved=同意 rejected=驳回 transferred=转交',
    comment         VARCHAR(1000) DEFAULT NULL COMMENT '审批意见',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_instance_id (instance_id),
    KEY idx_approver_id (approver_id),
    KEY idx_step_order (instance_id, step_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批节点记录表';

-- =====================================================
-- Initial Data: Process Templates
-- =====================================================
INSERT INTO sys_process_template (id, template_name, description, dept_id, form_id, step_chain, status) VALUES
(1, '请假审批', '员工请假审批流程', 101,
 1,
 '[{"order":1,"name":"部门经理审批","approverType":"dept_leader","approverValue":"101"},{"order":2,"name":"总经理审批","approverType":"role","approverValue":"manager","condition":"leave_days > 3"}]',
 0),
(2, '费用报销审批', '员工费用报销审批流程', NULL,
 2,
 '[{"order":1,"name":"部门经理审批","approverType":"dept_leader","approverValue":""},{"order":2,"name":"财务审批","approverType":"role","approverValue":"finance"},{"order":3,"name":"总经理审批","approverType":"role","approverValue":"manager","condition":"amount > 5000"}]',
 0);

-- =====================================================
-- Additional Menus: 审批中心
-- =====================================================
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, perms, menu_type, icon, sort, visible, status) VALUES
(40, 0,  '审批中心',   '/approval',              NULL,                           NULL,                     'M', 'audit',        6, 0, 0),
(41, 40, '模板管理',   '/approval/template',     'approval/template/index',      'approval:template:list', 'C', 'file-text',    1, 0, 0),
(42, 41, '模板新增',   '',                       NULL,                           'approval:template:add',  'F', '#',            1, 0, 0),
(43, 41, '模板修改',   '',                       NULL,                           'approval:template:edit', 'F', '#',            2, 0, 0),
(44, 41, '模板删除',   '',                       NULL,                           'approval:template:del',  'F', '#',            3, 0, 0),
(45, 40, '我的审批',   '/approval/todo',         'approval/todo/index',          'approval:todo:list',     'C', 'check-circle', 2, 0, 0),
(46, 40, '我的申请',   '/approval/my',           'approval/my/index',            'approval:my:list',       'C', 'form',         3, 0, 0),
(47, 40, '新建申请',   '/approval/start',        'approval/start/index',         'approval:start:add',     'C', 'plus-circle',   4, 0, 0);

-- Grant admin (role_id=1) all new menus
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id >= 40;

-- =====================================================
-- Done. Flowable ACT_* tables will be auto-created on first app startup.
-- =====================================================
