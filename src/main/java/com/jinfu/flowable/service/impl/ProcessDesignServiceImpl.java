package com.jinfu.flowable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.ResultCode;
import com.jinfu.flowable.dto.ProcessDesignSaveRequest;
import com.jinfu.flowable.dto.ProcessDesignVO;
import com.jinfu.flowable.entity.ProcessDesign;
import com.jinfu.flowable.mapper.ProcessDesignMapper;
import com.jinfu.flowable.service.ProcessDesignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessDesignServiceImpl
        extends ServiceImpl<ProcessDesignMapper, ProcessDesign>
        implements ProcessDesignService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;

    private final RepositoryService repositoryService;

    @Override
    public IPage<ProcessDesignVO> pageDesigns(Page<ProcessDesign> page, String keyword, Integer status) {
        LambdaQueryWrapper<ProcessDesign> wrapper = new LambdaQueryWrapper<ProcessDesign>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(ProcessDesign::getProcessName, keyword)
                        .or().like(ProcessDesign::getProcessKey, keyword))
                .eq(status != null, ProcessDesign::getStatus, status)
                .orderByDesc(ProcessDesign::getUpdateTime);
        IPage<ProcessDesign> resultPage = page(page, wrapper);
        return resultPage.convert(this::toVO);
    }

    @Override
    public ProcessDesign getDetail(Long id) {
        ProcessDesign design = getById(id);
        if (design == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "流程设计不存在或已删除");
        }
        return design;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDesign(ProcessDesignSaveRequest request) {
        if (request.getId() == null) {
            ProcessDesign design = new ProcessDesign();
            design.setProcessName(request.getProcessName());
            design.setProcessKey(request.getProcessKey());
            design.setBpmnXml(request.getBpmnXml());
            design.setRemark(request.getRemark());
            design.setStatus(STATUS_DRAFT);
            save(design);
            log.info("Created process design: {} ({})", design.getProcessName(), design.getId());
            return design.getId();
        }

        ProcessDesign existing = getById(request.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "流程设计不存在或已删除");
        }
        // 已发布的设计内容被修改后，标记回草稿，提示需要重新发布才生效（先取旧值再覆盖）
        boolean xmlChanged = STATUS_PUBLISHED == existing.getStatus()
                && !request.getBpmnXml().equals(existing.getBpmnXml());
        existing.setProcessName(request.getProcessName());
        existing.setProcessKey(request.getProcessKey());
        existing.setBpmnXml(request.getBpmnXml());
        existing.setRemark(request.getRemark());
        if (xmlChanged) {
            existing.setStatus(STATUS_DRAFT);
        }
        updateById(existing);
        return existing.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDesign(Long id) {
        ProcessDesign existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "流程设计不存在或已删除");
        }
        removeById(id);
        log.info("Deleted process design: {} ({})", existing.getProcessName(), id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDesign(Long id) {
        ProcessDesign design = getById(id);
        if (design == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "流程设计不存在或已删除");
        }
        try {
            Deployment deployment = repositoryService.createDeployment()
                    .name(design.getProcessName())
                    .addString(design.getProcessKey() + ".bpmn20.xml", design.getBpmnXml())
                    .deploy();
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();

            design.setStatus(STATUS_PUBLISHED);
            design.setDeploymentId(deployment.getId());
            if (pd != null) {
                design.setProcDefId(pd.getId());
                design.setVersion(pd.getVersion());
            }
            updateById(design);
            log.info("Published process design: {} -> deployment={}, version={}",
                    design.getProcessName(), deployment.getId(), design.getVersion());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to publish process design: {}", design.getProcessName(), e);
            throw new BusinessException(ResultCode.DEPLOY_FAILED, e.getMessage());
        }
    }

    private ProcessDesignVO toVO(ProcessDesign design) {
        ProcessDesignVO vo = new ProcessDesignVO();
        vo.setId(design.getId());
        vo.setProcessName(design.getProcessName());
        vo.setProcessKey(design.getProcessKey());
        vo.setStatus(design.getStatus());
        vo.setDeploymentId(design.getDeploymentId());
        vo.setProcDefId(design.getProcDefId());
        vo.setVersion(design.getVersion());
        vo.setRemark(design.getRemark());
        vo.setCreateBy(design.getCreateBy());
        vo.setUpdateBy(design.getUpdateBy());
        vo.setCreateTime(design.getCreateTime());
        vo.setUpdateTime(design.getUpdateTime());
        return vo;
    }
}
