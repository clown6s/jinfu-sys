package com.jinfu.flowable.event;

import lombok.Getter;

/**
 * 审批流程到达终态事件（approved / rejected / cancelled）
 * 由 ApprovalBridgeService 在流程结束时发布，
 * 业务模块（如日报）监听后联动更新自身状态。
 *
 * 迁移说明：原位于 com.jinfu.approval.event，随自研审批链删除迁移至此。
 */
@Getter
public class ApprovalFinishedEvent {

    /** 审批业务请求ID（sys_approval_request.id） */
    private final Long requestId;

    /** Flowable 流程实例ID */
    private final String processInstanceId;

    /** approved=已通过 rejected=已驳回 cancelled=已撤销 */
    private final String status;

    /** 审批标题 */
    private final String title;

    /** 发起人ID */
    private final Long initiatorId;

    public ApprovalFinishedEvent(Long requestId, String processInstanceId,
                                 String status, String title, Long initiatorId) {
        this.requestId = requestId;
        this.processInstanceId = processInstanceId;
        this.status = status;
        this.title = title;
        this.initiatorId = initiatorId;
    }
}
