package com.jinfu.daily.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinfu.approval.dto.StartProcessRequest;
import com.jinfu.approval.entity.SysProcessTemplate;
import com.jinfu.approval.event.ApprovalFinishedEvent;
import com.jinfu.approval.mapper.SysProcessTemplateMapper;
import com.jinfu.approval.service.ProcessInstanceService;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.ResultCode;
import com.jinfu.daily.dto.DailyReportVO;
import com.jinfu.daily.dto.DailySubmitRequest;
import com.jinfu.daily.entity.DailyFormConfig;
import com.jinfu.daily.entity.DailyReport;
import com.jinfu.daily.entity.LogType;
import com.jinfu.daily.mapper.DailyFormConfigMapper;
import com.jinfu.daily.mapper.DailyReportMapper;
import com.jinfu.daily.mapper.LogTypeMapper;
import com.jinfu.daily.service.DailyReportService;
import com.jinfu.form.entity.FormDefinition;
import com.jinfu.form.mapper.FormDefinitionMapper;
import com.jinfu.system.entity.SysDept;
import com.jinfu.system.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyReportServiceImpl
        extends ServiceImpl<DailyReportMapper, DailyReport>
        implements DailyReportService {

    private final DailyFormConfigMapper configMapper;
    private final FormDefinitionMapper formDefinitionMapper;
    private final SysDeptMapper deptMapper;
    private final SysProcessTemplateMapper templateMapper;
    private final LogTypeMapper logTypeMapper;
    private final ProcessInstanceService processInstanceService;

    @Override
    public DailyReportVO myForm(Long userId, Long deptId, Long logTypeId) {
        DailyFormConfig config = findEnabledConfig(deptId, logTypeId);

        DailyReportVO vo = new DailyReportVO();
        vo.setUserId(userId);
        vo.setDeptId(deptId);
        vo.setReportTime(config.getReportTime());
        vo.setProcessTemplateId(config.getProcessTemplateId());

        // 表单信息
        FormDefinition formDef = formDefinitionMapper.selectById(config.getFormId());
        if (formDef == null) {
            throw new BusinessException(ResultCode.FORM_DEF_NOT_FOUND, "部门日报表单不存在");
        }
        vo.setFormId(formDef.getId());
        vo.setFormName(formDef.getName());
        vo.setSchemaJson(formDef.getSchemaJson());

        if (config.getProcessTemplateId() != null) {
            SysProcessTemplate template = templateMapper.selectById(config.getProcessTemplateId());
            vo.setTemplateName(template != null ? template.getTemplateName() : null);
        }

        // 今日是否已提交
        DailyReport today = getOne(new LambdaQueryWrapper<DailyReport>()
                .eq(DailyReport::getUserId, userId)
                .eq(DailyReport::getReportDate, LocalDate.now())
                .last("LIMIT 1"));
        if (today != null) {
            vo.setTodaySubmitted(true);
            vo.setTodayReportId(today.getId());
            vo.setStatus(today.getStatus());
        } else {
            vo.setTodaySubmitted(false);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyReportVO submit(DailySubmitRequest request, Long userId, String userName, Long deptId, Long logTypeId) {
        DailyFormConfig config = findEnabledConfig(deptId, logTypeId);

        LocalDate reportDate = request.getReportDate() != null ? request.getReportDate() : LocalDate.now();

        // 当日查重
        Long exists = count(new LambdaQueryWrapper<DailyReport>()
                .eq(DailyReport::getUserId, userId)
                .eq(DailyReport::getReportDate, reportDate));
        if (exists > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "今日日报已提交，不能重复提交");
        }

        // 保存日志记录
        DailyReport report = new DailyReport();
        report.setUserId(userId);
        report.setUserName(userName);
        report.setLogTypeId(logTypeId);
        report.setDeptId(deptId);
        report.setFormId(config.getFormId());
        report.setReportDate(reportDate);
        report.setDataJson(JSONUtil.toJsonStr(request.getFormData()));
        report.setSubmitTime(LocalDateTime.now());
        // 配置了审批模板 → 待审批；否则直接提交成功
        report.setStatus(config.getProcessTemplateId() != null ? "pending" : "submitted");
        save(report);

        // 自动发起审批（审批通过/驳回通过事件联动更新日报状态）
        if (config.getProcessTemplateId() != null) {
            StartProcessRequest startReq = new StartProcessRequest();
            startReq.setTemplateId(config.getProcessTemplateId());
            startReq.setTitle(String.format("日报-%s-%s", userName, reportDate));
            startReq.setFormData(request.getFormData());
            var vo = processInstanceService.startProcess(
                    startReq, userId, userName, deptId);
            report.setApprovalInstId(vo.getId());
            updateById(report);
        }

        return toVO(report);
    }

    @Override
    public IPage<DailyReportVO> myReports(Page<DailyReport> page, Long userId, Long logTypeId) {
        LambdaQueryWrapper<DailyReport> wrapper = new LambdaQueryWrapper<DailyReport>()
                .eq(DailyReport::getUserId, userId);
        if (logTypeId != null) {
            wrapper.eq(DailyReport::getLogTypeId, logTypeId);
        }
        wrapper.orderByDesc(DailyReport::getReportDate);
        IPage<DailyReport> resultPage = page(page, wrapper);
        return resultPage.convert(this::toVO);
    }

    @Override
    public DailyReportVO detail(Long id, Long userId) {
        DailyReport report = getById(id);
        if (report == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "日报不存在");
        }
        if (!userId.equals(report.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能查看自己的日报");
        }
        return toVO(report);
    }

    /**
     * 监听审批终态：日报绑定的审批实例结束（通过/驳回/撤销）后联动更新日报状态。
     * approved→已通过 rejected→已驳回 cancelled→审批被撤销，日报回到已提交
     */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalFinished(ApprovalFinishedEvent event) {
        DailyReport report = getOne(new LambdaQueryWrapper<DailyReport>()
                .eq(DailyReport::getApprovalInstId, event.getInstanceId())
                .last("LIMIT 1"));
        if (report == null) {
            // 不是日报发起的审批，无需联动
            return;
        }
        String status = switch (event.getStatus()) {
            case "approved" -> "approved";
            case "rejected" -> "rejected";
            default -> "submitted";
        };
        report.setStatus(status);
        updateById(report);
        log.info("日报[{}] 关联审批实例[{}] 结束, 状态联动为 [{}]", report.getId(), event.getInstanceId(), status);
    }

    // ==================== 私有方法 ====================

    private DailyFormConfig findEnabledConfig(Long deptId, Long logTypeId) {
        DailyFormConfig config = configMapper.selectOne(new LambdaQueryWrapper<DailyFormConfig>()
                .eq(DailyFormConfig::getDeptId, deptId)
                .eq(DailyFormConfig::getLogTypeId, logTypeId)
                .last("LIMIT 1"));
        if (config == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "本部门未配置该类型日志表单，请联系管理员");
        }
        if (config.getEnabled() == null || config.getEnabled() != 1) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "本部门该类型日志填报已停用");
        }
        return config;
    }

    private DailyReportVO toVO(DailyReport report) {
        DailyReportVO vo = new DailyReportVO();
        vo.setId(report.getId());
        vo.setLogTypeId(report.getLogTypeId());
        vo.setUserId(report.getUserId());
        vo.setUserName(report.getUserName());
        vo.setDeptId(report.getDeptId());
        vo.setFormId(report.getFormId());
        vo.setReportDate(report.getReportDate());
        vo.setDataJson(report.getDataJson());
        vo.setStatus(report.getStatus());
        vo.setApprovalInstId(report.getApprovalInstId());
        vo.setSubmitTime(report.getSubmitTime());

        if (report.getLogTypeId() != null) {
            LogType logType = logTypeMapper.selectById(report.getLogTypeId());
            vo.setLogTypeName(logType != null ? logType.getName() : null);
        }

        SysDept dept = report.getDeptId() != null ? deptMapper.selectById(report.getDeptId()) : null;
        vo.setDeptName(dept != null ? dept.getDeptName() : null);

        FormDefinition formDef = report.getFormId() != null
                ? formDefinitionMapper.selectById(report.getFormId()) : null;
        if (formDef != null) {
            vo.setFormName(formDef.getName());
            vo.setSchemaJson(formDef.getSchemaJson());
        }
        return vo;
    }
}
