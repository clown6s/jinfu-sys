package com.jinfu.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinfu.approval.dto.CompleteApprovalRequest;
import com.jinfu.approval.dto.ProcessInstanceVO;
import com.jinfu.approval.dto.StartProcessRequest;
import com.jinfu.approval.entity.SysProcessInstance;

public interface ProcessInstanceService extends IService<SysProcessInstance> {

    /**
     * 发起审批
     */
    ProcessInstanceVO startProcess(StartProcessRequest request, Long userId, String userName, Long deptId);

    /**
     * 审批操作（同意/拒绝）
     */
    void completeApproval(CompleteApprovalRequest request, Long userId, String userName);

    /**
     * 撤销审批
     */
    void cancelProcess(Long instanceId, Long userId);

    /**
     * 获取审批详情（含节点记录）
     */
    ProcessInstanceVO getDetail(Long instanceId);

    /**
     * 我的申请（分页）
     */
    IPage<ProcessInstanceVO> myApplications(Page<SysProcessInstance> page, Long userId);

    /**
     * 待我审批（分页）
     */
    IPage<ProcessInstanceVO> todoApprovals(Page<SysProcessInstance> page, Long userId);

    /**
     * 我已审批（分页）
     */
    IPage<ProcessInstanceVO> doneApprovals(Page<SysProcessInstance> page, Long userId);
}
