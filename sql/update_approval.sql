-- =====================================================
-- 在线更新脚本：新增审批流程模块
-- 在已有 jinfu_sys 数据库上执行
-- =====================================================

USE jinfu_sys;

-- 10. 审批流程模板表
CREATE TABLE IF NOT EXISTS sys_process_template (
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

-- 11. 审批流程实例表
CREATE TABLE IF NOT EXISTS sys_process_instance (
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

-- 12. 审批节点记录表
CREATE TABLE IF NOT EXISTS sys_approval_node (
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

-- 新增菜单
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, perms, menu_type, icon, sort, visible, status) VALUES
(40, 0,  '审批中心',   '/approval',              NULL,                           NULL,                     'M', 'audit',        6, 0, 0),
(41, 40, '模板管理',   '/approval/template',     'approval/template/index',      'approval:template:list', 'C', 'file-text',    1, 0, 0),
(42, 41, '模板新增',   '',                       NULL,                           'approval:template:add',  'F', '#',            1, 0, 0),
(43, 41, '模板修改',   '',                       NULL,                           'approval:template:edit', 'F', '#',            2, 0, 0),
(44, 41, '模板删除',   '',                       NULL,                           'approval:template:del',  'F', '#',            3, 0, 0),
(45, 40, '我的审批',   '/approval/todo',         'approval/todo/index',          'approval:todo:list',     'C', 'check-circle', 2, 0, 0),
(46, 40, '我的申请',   '/approval/my',           'approval/my/index',            'approval:my:list',       'C', 'form',         3, 0, 0),
(47, 40, '新建申请',   '/approval/start',        'approval/start/index',         'approval:start:add',     'C', 'plus-circle',   4, 0, 0);

-- 授权：管理员(role_id=1)所有新菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id >= 40;

-- 种子数据：默认审批模板
INSERT IGNORE INTO sys_process_template (id, template_name, description, dept_id, form_id, step_chain, status) VALUES
(1, '请假审批', '员工请假审批流程', 101,
 1,
 '[{"order":1,"name":"部门经理审批","approverType":"dept_leader","approverValue":"101"},{"order":2,"name":"总经理审批","approverType":"role","approverValue":"manager","condition":"leave_days > 3"}]',
 0),
(2, '费用报销审批', '员工费用报销审批流程', NULL,
 2,
 '[{"order":1,"name":"部门经理审批","approverType":"dept_leader","approverValue":""},{"order":2,"name":"财务审批","approverType":"role","approverValue":"admin"},{"order":3,"name":"总经理审批","approverType":"role","approverValue":"manager","condition":"amount > 5000"}]',
 0);
