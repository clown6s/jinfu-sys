-- =====================================================
-- JinFu OA - 增量脚本 2026-08-01
-- 1) 消息中心（站内信）: sys_message
-- 2) 部门日报配置: daily_form_config
-- 3) 日报记录: daily_report
-- 4) 新增菜单（日报/消息中心）
-- 说明: 在已初始化的库上执行；全新库直接跑 jinfu_sys.sql（已含下述内容）
-- =====================================================

USE jinfu_sys;

-- =====================================================
-- 1. sys_message (站内信)
-- =====================================================
CREATE TABLE IF NOT EXISTS sys_message (
    id          BIGINT       NOT NULL COMMENT 'Primary Key',
    user_id     BIGINT       NOT NULL COMMENT '接收用户ID',
    msg_type    VARCHAR(20)  NOT NULL DEFAULT 'system' COMMENT 'approval=审批 cc=抄送 daily=日报 system=系统',
    title       VARCHAR(200) NOT NULL COMMENT '消息标题',
    content     VARCHAR(2000) DEFAULT NULL COMMENT '消息内容',
    biz_id      BIGINT       DEFAULT NULL COMMENT '关联业务ID（审批实例ID等）',
    read_flag   TINYINT      NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读',
    read_time   DATETIME     DEFAULT NULL COMMENT '阅读时间',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_read (user_id, read_flag),
    KEY idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

-- =====================================================
-- 2. daily_form_config (部门日报配置)
-- =====================================================
CREATE TABLE IF NOT EXISTS daily_form_config (
    id                 BIGINT       NOT NULL COMMENT 'Primary Key',
    dept_id            BIGINT       NOT NULL COMMENT '部门ID（唯一绑定）',
    form_id            BIGINT       NOT NULL COMMENT '关联表单定义ID（各部门日报表单可不同）',
    process_template_id BIGINT      DEFAULT NULL COMMENT '关联审批模板ID（NULL=日报不需审批）',
    report_time        VARCHAR(5)   DEFAULT '18:00' COMMENT '填报截止时间 HH:mm',
    enabled            TINYINT      DEFAULT 1 COMMENT '0=停用 1=启用',
    create_by          BIGINT       DEFAULT NULL,
    create_time        DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by          BIGINT       DEFAULT NULL,
    update_time        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted            TINYINT      DEFAULT 0 COMMENT '0=存在 1=删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dept (dept_id),
    KEY idx_form (form_id),
    KEY idx_template (process_template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门日报表单配置表';

-- =====================================================
-- 3. daily_report (日报记录)
-- =====================================================
CREATE TABLE IF NOT EXISTS daily_report (
    id              BIGINT       NOT NULL COMMENT 'Primary Key',
    user_id         BIGINT       NOT NULL COMMENT '填报人ID',
    user_name       VARCHAR(50)  DEFAULT NULL COMMENT '填报人姓名快照',
    dept_id         BIGINT       NOT NULL COMMENT '填报人部门ID',
    form_id         BIGINT       NOT NULL COMMENT '表单定义ID',
    report_date     DATE         NOT NULL COMMENT '填报日期（YYYY-MM-DD）',
    data_json       LONGTEXT     COMMENT '表单数据快照',
    status          VARCHAR(20)  NOT NULL DEFAULT 'submitted' COMMENT 'submitted=已提交 pending=审批中 approved=已通过 rejected=已驳回',
    approval_inst_id BIGINT      DEFAULT NULL COMMENT '关联审批实例ID（sys_process_instance.id）',
    submit_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    create_by       BIGINT       DEFAULT NULL,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT       DEFAULT NULL,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      DEFAULT 0 COMMENT '0=存在 1=删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_date (user_id, report_date),
    KEY idx_dept_date (dept_id, report_date),
    KEY idx_approval_inst (approval_inst_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日报记录表';

-- =====================================================
-- 4. 新增菜单
-- =====================================================
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, perms, menu_type, icon, sort, visible, status) VALUES
-- 消息中心（隐藏菜单，仅权限控制，入口在顶部铃铛）
(60, 0, '消息中心', '/message', 'message/index', 'message:list', 'C', 'message', 7, 1, 0),
-- 日报管理
(61, 0, '日报管理', '/daily', NULL, NULL, 'M', 'schedule', 6, 0, 0),
(62, 61, '日报配置', '/daily/config', 'daily/config/index', 'daily:config:list', 'C', 'setting', 1, 0, 0),
(63, 62, '配置新增', '', NULL, 'daily:config:add', 'F', '#', 1, 0, 0),
(64, 62, '配置修改', '', NULL, 'daily:config:edit', 'F', '#', 2, 0, 0),
(65, 62, '配置删除', '', NULL, 'daily:config:del', 'F', '#', 3, 0, 0),
(66, 61, '我的日报', '/daily/my', 'daily/my/index', 'daily:report:list', 'C', 'form', 2, 0, 0),
(67, 66, '提交日报', '', NULL, 'daily:report:add', 'F', '#', 1, 0, 0);

-- 授权 admin 角色（role_id=1）所有新菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id >= 60;
