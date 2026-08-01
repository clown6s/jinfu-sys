package com.jinfu.flowable.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程设计列表项（不含 bpmnXml，保证分页轻量）
 */
@Data
public class ProcessDesignVO {

    private Long id;
    private String processName;
    private String processKey;
    /** 0=草稿 1=已发布 */
    private Integer status;
    private String deploymentId;
    private String procDefId;
    private Integer version;
    private String remark;
    private Long createBy;
    private Long updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
