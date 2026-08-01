package com.jinfu.daily.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日报视图对象（my-form 查询 / 列表 / 详情复用）
 */
@Data
public class DailyReportVO {

    /** 日报记录ID */
    private Long id;

    /** 填报人 */
    private Long userId;
    private String userName;

    /** 部门 */
    private Long deptId;
    private String deptName;

    /** 表单（渲染用） */
    private Long formId;
    private String formName;
    private String schemaJson;

    /** 日报配置 */
    private String reportTime;
    private Long processTemplateId;
    private String templateName;

    /** 填报日期与数据 */
    private LocalDate reportDate;
    private String dataJson;
    private String status;
    private Long approvalInstId;
    private LocalDateTime submitTime;

    /** 今日是否已提交（my-form 查询用） */
    private Boolean todaySubmitted;
    private Long todayReportId;
}
