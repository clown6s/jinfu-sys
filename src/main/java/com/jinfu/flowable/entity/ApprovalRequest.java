package com.jinfu.flowable.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jinfu.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批业务请求 — 补齐 Flowable 不管理的业务数据：
 * 标题、表单 Schema 快照、表单数据、发起人、抄送人。
 * 通过 processInstanceId 与 Flowable ACT_RU_EXECUTION 关联。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_approval_request")
public class ApprovalRequest extends BaseEntity {

    /** Flowable 流程实例ID */
    private String processInstanceId;

    /** 流程定义Key */
    private String processKey;

    /** 业务Key（如 daily_report:{reportId}） */
    private String businessKey;

    /** 审批标题 */
    private String title;

    /** 表单定义ID */
    private Long formId;

    /** 表单 Schema 快照（发起时的 schema，防止后续修改影响在途审批） */
    private String formSchemaSnapshot;

    /** 表单数据 JSON */
    private String formData;

    /** 发起人ID */
    private Long initiatorId;

    /** 发起人姓名 */
    private String initiatorName;

    /** 发起人部门ID */
    private Long deptId;

    /** 抄送人ID列表（JSON 数组，如 [1,2,3]） */
    private String ccUserIds;
}
