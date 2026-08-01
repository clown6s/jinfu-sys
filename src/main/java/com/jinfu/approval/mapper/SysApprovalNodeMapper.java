package com.jinfu.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinfu.approval.entity.SysApprovalNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysApprovalNodeMapper extends BaseMapper<SysApprovalNode> {

    /**
     * 根据实例ID查询所有审批节点
     */
    List<SysApprovalNode> selectByInstanceId(@Param("instanceId") Long instanceId);

    /**
     * 查询某用户待审批的节点列表
     */
    List<SysApprovalNode> selectPendingByUserId(@Param("userId") Long userId);
}
