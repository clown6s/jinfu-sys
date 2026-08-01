package com.jinfu.flowable.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jinfu.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程设计草稿 — 设计器画布内容持久化；发布时才部署到 Flowable ACT_* 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_process_design")
public class ProcessDesign extends BaseEntity {

    /** 流程名称 */
    private String processName;

    /** 流程Key（BPMN process id，Flowable 按它升版本） */
    private String processKey;

    /** BPMN 2.0 XML */
    private String bpmnXml;

    /** 0=草稿 1=已发布 */
    private Integer status;

    /** Flowable 部署ID */
    private String deploymentId;

    /** Flowable 流程定义ID */
    private String procDefId;

    /** 已发布版本号 */
    private Integer version;

    /** 备注 */
    private String remark;
}
