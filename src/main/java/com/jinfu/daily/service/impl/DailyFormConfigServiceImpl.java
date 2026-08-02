package com.jinfu.daily.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.ResultCode;
import com.jinfu.daily.dto.DailyConfigVO;
import com.jinfu.daily.entity.DailyFormConfig;
import com.jinfu.daily.entity.LogType;
import com.jinfu.daily.mapper.DailyFormConfigMapper;
import com.jinfu.daily.mapper.LogTypeMapper;
import com.jinfu.daily.service.DailyFormConfigService;
import com.jinfu.flowable.entity.ProcessDesign;
import com.jinfu.flowable.service.ProcessDesignService;
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
    private final ProcessDesignService processDesignService;
    private final LogTypeMapper logTypeMapper;

    @Override
    public IPage<DailyConfigVO> pageConfigs(Page<DailyFormConfig> page, String keyword) {
        LambdaQueryWrapper<DailyFormConfig> wrapper = new LambdaQueryWrapper<DailyFormConfig>()
                .orderByAsc(DailyFormConfig::getDeptId);
        IPage<DailyFormConfig> resultPage = page(page, wrapper);
        IPage<DailyConfigVO> voPage = resultPage.convert(this::toVO);
        // keyword 在 VO 层过滤（匹配部门名、日志类型名、表单名）
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim().toLowerCase();
            List<DailyConfigVO> filtered = voPage.getRecords().stream()
                    .filter(vo -> containsIgnoreCase(vo.getDeptName(), kw)
                            || containsIgnoreCase(vo.getLogTypeName(), kw)
                            || containsIgnoreCase(vo.getFormName(), kw)
                            || containsIgnoreCase(vo.getTemplateName(), kw))
                    .toList();
            voPage.setRecords(filtered);
            voPage.setTotal(filtered.size());
        }
        return voPage;
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        return text != null && text.toLowerCase().contains(keyword);
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
        if (lambdaQuery()
                .eq(DailyFormConfig::getDeptId, config.getDeptId())
                .eq(DailyFormConfig::getLogTypeId, config.getLogTypeId())
                .exists()) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "该部门已配置该类型日志表单");
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
        // 部门或类型变更时查重（联合唯一：deptId + logTypeId）
        boolean deptChanged = config.getDeptId() != null && !config.getDeptId().equals(existing.getDeptId());
        boolean typeChanged = config.getLogTypeId() != null && !config.getLogTypeId().equals(existing.getLogTypeId());
        if (deptChanged || typeChanged) {
            Long checkDeptId = deptChanged ? config.getDeptId() : existing.getDeptId();
            Long checkTypeId = typeChanged ? config.getLogTypeId() : existing.getLogTypeId();
            if (lambdaQuery()
                    .eq(DailyFormConfig::getDeptId, checkDeptId)
                    .eq(DailyFormConfig::getLogTypeId, checkTypeId)
                    .exists()) {
                throw new BusinessException(ResultCode.DUPLICATE_KEY, "该部门已配置该类型日志表单");
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
        vo.setLogTypeId(config.getLogTypeId());
        vo.setDeptId(config.getDeptId());
        vo.setFormId(config.getFormId());
        vo.setProcessTemplateId(config.getProcessTemplateId());
        vo.setProcessKey(config.getProcessKey());
        vo.setReportTime(config.getReportTime());
        vo.setEnabled(config.getEnabled());

        if (config.getLogTypeId() != null) {
            LogType logType = logTypeMapper.selectById(config.getLogTypeId());
            vo.setLogTypeName(logType != null ? logType.getName() : null);
        }
        if (config.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(config.getDeptId());
            vo.setDeptName(dept != null ? dept.getDeptName() : null);
        }
        if (config.getFormId() != null) {
            FormDefinition formDef = formDefinitionMapper.selectById(config.getFormId());
            vo.setFormName(formDef != null ? formDef.getName() : null);
        }
        // 通过 processKey 查流程设计名称展示
        if (config.getProcessKey() != null && !config.getProcessKey().isEmpty()) {
            ProcessDesign design = processDesignService.getOne(
                    new LambdaQueryWrapper<ProcessDesign>()
                            .eq(ProcessDesign::getProcessKey, config.getProcessKey())
                            .orderByDesc(ProcessDesign::getVersion)
                            .last("LIMIT 1"));
            vo.setTemplateName(design != null ? design.getProcessName() : config.getProcessKey());
        }
        return vo;
    }
}
