package com.jinfu.flowable.dto;

import lombok.Data;

import java.util.Date;

@Data
public class ProcessInstanceVO {
    private String id;
    private String processDefinitionId;
    private String processDefinitionName;
    private String businessKey;
    private String startUserId;
    private Date startTime;
    private Date endTime;
    private String deleteReason;
}
