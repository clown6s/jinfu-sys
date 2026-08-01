package com.jinfu.flowable.dto;

import lombok.Data;

import java.util.Date;

@Data
public class TaskVO {
    private String id;
    private String name;
    private String processInstanceId;
    private String processDefinitionId;
    private String processDefinitionName;
    private String assignee;
    private String starter;
    private Date createTime;
    private String businessKey;
    private String formKey;
}
