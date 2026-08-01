package com.jinfu.flowable.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinfu.flowable.dto.ProcessDesignSaveRequest;
import com.jinfu.flowable.dto.ProcessDesignVO;
import com.jinfu.flowable.entity.ProcessDesign;

/**
 * 流程设计草稿服务
 */
public interface ProcessDesignService extends IService<ProcessDesign> {

    /** 分页列表（不含 XML） */
    IPage<ProcessDesignVO> pageDesigns(Page<ProcessDesign> page, String keyword, Integer status);

    /** 详情（含 BPMN XML，供设计器加载） */
    ProcessDesign getDetail(Long id);

    /** 保存（id 为空=新建返回新 id，非空=更新）；已发布的设计 XML 变更后状态回退为草稿 */
    Long saveDesign(ProcessDesignSaveRequest request);

    /** 删除草稿（逻辑删除，不影响已发布的流程定义） */
    void removeDesign(Long id);

    /** 发布：部署到 Flowable，回写 deploymentId/procDefId/version，status=1 */
    void publishDesign(Long id);
}
