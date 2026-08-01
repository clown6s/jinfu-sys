package com.jinfu.approval.event;

import lombok.Getter;

/**
 * 审批流程到达终态事件（approved / rejected / cancelled）
 * 由 ProcessInstanceService 在流程结束时发布，
 * 业务模块（如日报）监听后联动更新自身状态。
 */
@Getter
public class ApprovalFinishedEvent {

    /** 审批实例ID（sys_process_instance.id） */
    private final Long instanceId;

    /** approved=已通过 rejected=已驳回 cancelled=已撤销 */
    private final String status;

    /** 审批标题 */
    private final String title;

    /** 发起人ID */
    private final Long initiatorId;

    public ApprovalFinishedEvent(Long instanceId, String status, String title, Long initiatorId) {
        this.instanceId = instanceId;
        this.status = status;
        this.title = title;
        this.initiatorId = initiatorId;
    }
}
