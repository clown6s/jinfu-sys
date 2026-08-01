package com.jinfu.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinfu.approval.entity.SysProcessInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysProcessInstanceMapper extends BaseMapper<SysProcessInstance> {

    /**
     * 分页查询我的申请
     */
    IPage<SysProcessInstance> selectMyApplications(Page<SysProcessInstance> page, @Param("userId") Long userId);

    /**
     * 分页查询待我审批的实例（根据审批节点表关联）
     */
    IPage<SysProcessInstance> selectTodoApprovals(Page<SysProcessInstance> page, @Param("userId") Long userId);

    /**
     * 分页查询我已审批的实例
     */
    IPage<SysProcessInstance> selectDoneApprovals(Page<SysProcessInstance> page, @Param("userId") Long userId);
}
