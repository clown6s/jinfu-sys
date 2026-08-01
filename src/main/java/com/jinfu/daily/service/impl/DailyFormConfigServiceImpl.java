package com.jinfu.daily.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinfu.approval.entity.SysProcessTemplate;
import com.jinfu.approval.mapper.SysProcessTemplateMapper;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.ResultCode;
import com.jinfu.daily.dto.DailyConfigVO;
import com.jinfu.daily.entity.DailyFormConfig;
import com.jinfu.daily.mapper.DailyFormConfigMapper;
import com.jinfu.daily.service.DailyFormConfigService;
import com.jinfu.form.entity.FormDefinition;
import com.jinfu.form.mapper.FormDefinitionMapper;
import com.jinfu.system.entity.SysDept;
import com.jinfu.system.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyFormConfigServiceImpl
        extends ServiceImpl<DailyFormConfigMapper, DailyFormConfig>
        implements DailyFormConfigService {

    private final SysDeptMapper deptMapper;
    private final FormDefinitionMapper formDefinitionMapper;
    private final SysProcessTemplateMapper templateMapper;

    @Override
    public IPage<DailyConfigVO> pageConfigs(Page<DailyFormConfig> page, String keyword) {
        LambdaQueryWrapper<DailyFormConfig> wrapper = new LambdaQueryWrapper<DailyFormConfig>()
                .orderByAsc(DailyFormConfig::getDeptId);
        IPage<DailyFormConfig> resultPage = page(page, wrapper);
        return resultPage.convert(this::toVO);
    }

    @Override
    public List<DailyConfigVO> listConfigs() {
        return list(new LambdaQueryWrapper<DailyFormConfig>()
                        .orderByAsc(DailyFormConfig::getDeptId))
                .stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addConfig(DailyFormConfig config) {
        if (lambdaQuery().eq(DailyFormConfig::getDeptId, config.getDeptId()).exists()) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "该部门已配置日报表单");
        }
        if (config.getEnabled() == null) {
            config.setEnabled(1);
        }
        if (!StringUtils.hasText(config.getReportTime())) {
            config.setReportTime("18:00");
        }
        save(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(DailyFormConfig config) {
        DailyFormConfig existing = getById(config.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "日报配置不存在");
        }
        // 部门变更时查重
        if (config.getDeptId() != null && !config.getDeptId().equals(existing.getDeptId())) {
            if (lambdaQuery().eq(DailyFormConfig::getDeptId, config.getDeptId()).exists()) {
                throw new BusinessException(ResultCode.DUPLICATE_KEY, "该部门已配置日报表单");
            }
        }
        updateById(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeConfig(Long id) {
        DailyFormConfig existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "日报配置不存在");
        }
        removeById(id);
    }

    private DailyConfigVO toVO(DailyFormConfig config) {
        DailyConfigVO vo = new DailyConfigVO();
        vo.setId(config.getId());
        vo.setDeptId(config.getDeptId());
        vo.setFormId(config.getFormId());
        vo.setProcessTemplateId(config.getProcessTemplateId());
        vo.setReportTime(config.getReportTime());
        vo.setEnabled(config.getEnabled());

        if (config.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(config.getDeptId());
            vo.setDeptName(dept != null ? dept.getDeptName() : null);
        }
        if (config.getFormId() != null) {
            FormDefinition formDef = formDefinitionMapper.selectById(config.getFormId());
            vo.setFormName(formDef != null ? formDef.getName() : null);
        }
        if (config.getProcessTemplateId() != null) {
            SysProcessTemplate template = templateMapper.selectById(config.getProcessTemplateId());
            vo.setTemplateName(template != null ? template.getTemplateName() : null);
        }
        return vo;
    }
}
