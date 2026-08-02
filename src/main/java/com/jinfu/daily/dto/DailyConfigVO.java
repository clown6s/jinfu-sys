package com.jinfu.daily.dto;

import lombok.Data;

/**
 * 日报配置视图对象（带部门/表单/模板名称）
 */
@Data
public class DailyConfigVO {

    private Long id;
    private Long logTypeId;
    private String logTypeName;
    private Long deptId;
    private String deptName;
    private Long formId;
    private String formName;
    private Long processTemplateId;
    private String templateName;
    private String reportTime;
    private Integer enabled;
}
