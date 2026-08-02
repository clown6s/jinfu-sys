package com.jinfu.daily.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jinfu.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日报记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_report")
public class DailyReport extends BaseEntity {

    /** 填报人ID */
    private Long userId;

    /** 填报人姓名快照 */
    private String userName;

    /** 日志类型ID */
    private Long logTypeId;

    /** 填报人部门ID */
    private Long deptId;

    /** 表单定义ID */
    private Long formId;

    /** 填报日期（YYYY-MM-DD） */
    private LocalDate reportDate;

    /** 表单数据快照 */
    private String dataJson;

    /** submitted=已提交 pending=审批中 approved=已通过 rejected=已驳回 */
    private String status;

    /** 关联审批实例ID（sys_process_instance.id） */
    private Long approvalInstId;

    /** 提交时间 */
    private LocalDateTime submitTime;
}
