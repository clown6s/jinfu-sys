-- ==============================================
-- 日志类型扩展变更脚本
-- 日期: 2026-08-02
-- 说明: 新增 log_type 表，扩展 daily_form_config 和 daily_report 支持多日志类型
-- ==============================================

-- 0. 创建审批流程模板表（如果尚未存在）
CREATE TABLE IF NOT EXISTS sys_process_template (
    id              BIGINT          PRIMARY KEY COMMENT '主键ID',
    template_name   VARCHAR(100)    NOT NULL COMMENT '模板名称',
    description     VARCHAR(500)    DEFAULT NULL COMMENT '描述',
    dept_id         BIGINT          DEFAULT NULL COMMENT '所属部门ID（NULL=全公司可用）',
    form_id         BIGINT          DEFAULT NULL COMMENT '关联表单定义ID',
    step_chain      TEXT            DEFAULT NULL COMMENT '审批步骤链 JSON',
    status          TINYINT         DEFAULT 0 COMMENT '状态（0=启用 1=停用）',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除标志'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程模板表';

-- 1. 创建日志类型表
CREATE TABLE IF NOT EXISTS log_type (
    id              BIGINT          PRIMARY KEY COMMENT '主键ID',
    name            VARCHAR(50)     NOT NULL COMMENT '类型名称（如：日报、周报、月报）',
    code            VARCHAR(50)     NOT NULL COMMENT '类型编码（如：daily, weekly, monthly）',
    description     VARCHAR(200)    DEFAULT NULL COMMENT '描述',
    sort_order      INT             DEFAULT 0 COMMENT '排序',
    enabled         TINYINT         DEFAULT 1 COMMENT '启用状态（0=停用 1=启用）',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除标志',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志类型表';

-- 2. 插入默认日志类型
INSERT INTO log_type (id, name, code, description, sort_order, enabled) VALUES
(1, '日报', 'daily', '每日工作日报', 1, 1),
(2, '周报', 'weekly', '每周工作周报', 2, 1),
(3, '月报', 'monthly', '每月工作月报', 3, 1);

-- 3. 扩展 daily_form_config 表
ALTER TABLE daily_form_config
    ADD COLUMN log_type_id BIGINT DEFAULT 1 COMMENT '日志类型ID' AFTER dept_id;

-- 4. 添加联合唯一索引（部门 + 日志类型）
ALTER TABLE daily_form_config
    ADD UNIQUE INDEX uk_dept_log_type (dept_id, log_type_id);

-- 5. 扩展 daily_report 表
ALTER TABLE daily_report
    ADD COLUMN log_type_id BIGINT DEFAULT 1 COMMENT '日志类型ID' AFTER dept_id;

-- 6. 添加索引优化查询
ALTER TABLE daily_report
    ADD INDEX idx_log_type (log_type_id);

-- 7. 添加日志管理菜单（如果尚未存在）
-- 先插入父菜单（日志管理目录）
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, perms, menu_type, icon, sort, visible, status, create_time)
SELECT 1000, 0, '日志管理', '/daily', NULL, NULL, 'M', 'schedule', 50, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 1000);

-- 日志类型管理菜单
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, perms, menu_type, icon, sort, visible, status, create_time)
SELECT 1001, 1000, '日志类型', '/log-type', 'logType/index', 'daily:logType:list', 'C', 'form', 1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 1001);

-- 日报配置菜单（如果尚未存在）
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, perms, menu_type, icon, sort, visible, status, create_time)
SELECT 1002, 1000, '日志配置', '/daily/config', 'daily/config/index', 'daily:config:list', 'C', 'form', 2, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 1002);

-- 我的日志菜单（如果尚未存在）
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, perms, menu_type, icon, sort, visible, status, create_time)
SELECT 1003, 1000, '我的日志', '/daily/my', 'daily/my/index', 'daily:report:my', 'C', 'file-text', 3, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 1003);

-- ==============================================
-- 审批管理菜单
-- ==============================================

-- 审批管理目录（如果尚未存在）
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, perms, menu_type, icon, sort, visible, status, create_time)
SELECT 2000, 0, '审批管理', '/approval', NULL, NULL, 'M', 'form', 60, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 2000);

-- 审批模板菜单
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, perms, menu_type, icon, sort, visible, status, create_time)
SELECT 2001, 2000, '审批模板', '/approval/template', 'approval/template/index', 'approval:template:list', 'C', 'form', 1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 2001);

-- 发起审批菜单
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, perms, menu_type, icon, sort, visible, status, create_time)
SELECT 2002, 2000, '发起审批', '/approval/start', 'approval/start/index', 'approval:start:submit', 'C', 'form', 2, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 2002);

-- 待办审批菜单
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, perms, menu_type, icon, sort, visible, status, create_time)
SELECT 2003, 2000, '待办审批', '/approval/todo', 'approval/todo/index', 'approval:todo:list', 'C', 'form', 3, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 2003);

-- 我的审批菜单
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, perms, menu_type, icon, sort, visible, status, create_time)
SELECT 2004, 2000, '我的审批', '/approval/my', 'approval/my/index', 'approval:my:list', 'C', 'form', 4, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 2004);
