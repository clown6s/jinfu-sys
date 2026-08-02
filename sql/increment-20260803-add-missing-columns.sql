-- ============================================================
-- 安全补列脚本：补全 daily_form_config / daily_report 实体已有但表可能缺的列
-- 幂等：列已存在则跳过，不会报错。可在 Navicat 整段执行。
-- ============================================================

-- ---------- daily_form_config 补列 ----------

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_form_config'
  AND COLUMN_NAME = 'process_template_id') = 0,
  'ALTER TABLE daily_form_config ADD COLUMN process_template_id BIGINT NULL COMMENT ''关联审批模板ID（NULL=无需审批）''',
  'SELECT ''skip: process_template_id exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_form_config'
  AND COLUMN_NAME = 'process_key') = 0,
  'ALTER TABLE daily_form_config ADD COLUMN process_key VARCHAR(128) NULL COMMENT ''Flowable流程定义Key''',
  'SELECT ''skip: process_key exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- daily_report 补列 ----------

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_report'
  AND COLUMN_NAME = 'form_id') = 0,
  'ALTER TABLE daily_report ADD COLUMN form_id BIGINT NULL COMMENT ''表单定义ID''',
  'SELECT ''skip: form_id exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_report'
  AND COLUMN_NAME = 'data_json') = 0,
  'ALTER TABLE daily_report ADD COLUMN data_json TEXT NULL COMMENT ''表单数据快照''',
  'SELECT ''skip: data_json exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_report'
  AND COLUMN_NAME = 'status') = 0,
  'ALTER TABLE daily_report ADD COLUMN status VARCHAR(32) NULL DEFAULT ''submitted'' COMMENT ''submitted/pending/approved/rejected''',
  'SELECT ''skip: status exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_report'
  AND COLUMN_NAME = 'approval_inst_id') = 0,
  'ALTER TABLE daily_report ADD COLUMN approval_inst_id VARCHAR(64) NULL COMMENT ''Flowable流程实例ID''',
  'SELECT ''skip: approval_inst_id exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_report'
  AND COLUMN_NAME = 'submit_time') = 0,
  'ALTER TABLE daily_report ADD COLUMN submit_time DATETIME NULL COMMENT ''提交时间''',
  'SELECT ''skip: submit_time exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 验证：两张表当前的列 ----------
SELECT 'daily_form_config' AS tbl, COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_form_config';

SELECT 'daily_report' AS tbl, COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'daily_report';
