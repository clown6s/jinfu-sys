package com.jinfu.flowable.dto;

import lombok.Data;

import java.util.Date;

@Data
public class HistoryActivityVO {
    private String activityId;
    private String activityName;
    private String activityType;
    private String assignee;
    private Date startTime;
    private Date endTime;
    private Long durationInMillis;
    private String comment;
}
