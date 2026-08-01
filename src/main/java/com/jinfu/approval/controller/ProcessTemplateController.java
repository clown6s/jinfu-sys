package com.jinfu.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinfu.approval.dto.ProcessTemplateDTO;
import com.jinfu.approval.entity.SysProcessTemplate;
import com.jinfu.approval.service.ProcessTemplateService;
import com.jinfu.common.result.Result;
import com.jinfu.common.result.ResultCode;
import com.jinfu.form.entity.FormDefinition;
import com.jinfu.form.mapper.FormDefinitionMapper;
import com.jinfu.security.annotation.RequiresPermission;
import com.jinfu.system.entity.SysDept;
import com.jinfu.system.mapper.SysDeptMapper;
import cn.hutool.json.JSONUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/approval/template")
@RequiredArgsConstructor
@Tag(name = "审批模板管理")
public class ProcessTemplateController {

    private final ProcessTemplateService templateService;
    private final FormDefinitionMapper formDefinitionMapper;
    private final SysDeptMapper sysDeptMapper;

    @GetMapping("/list")
    @RequiresPermission("approval:template:list")
    @Operation(summary = "分页查询审批模板")
    public Result<IPage<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {

        Page<SysProcessTemplate> page = new Page<>(pageNum, pageSize);
        IPage<SysProcessTemplate> result = templateService.lambdaQuery()
                .like(keyword != null, SysProcessTemplate::getTemplateName, keyword)
                .or(keyword != null)
                .like(keyword != null, SysProcessTemplate::getDescription, keyword)
                .orderByDesc(SysProcessTemplate::getCreateTime)
                .page(page);

        // 转为带关联名称的 Map
        IPage<Map<String, Object>> enriched = result.convert(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("templateName", t.getTemplateName());
            m.put("description", t.getDescription());
            m.put("deptId", t.getDeptId());
            m.put("formId", t.getFormId());
            m.put("stepChain", t.getStepChain());
            m.put("status", t.getStatus());
            m.put("createTime", t.getCreateTime());

            // 关联名称
            if (t.getDeptId() != null) {
                SysDept dept = sysDeptMapper.selectById(t.getDeptId());
                m.put("deptName", dept != null ? dept.getDeptName() : "-");
            } else {
                m.put("deptName", "全公司");
            }
            if (t.getFormId() != null) {
                FormDefinition form = formDefinitionMapper.selectById(t.getFormId());
                m.put("formName", form != null ? form.getName() : "-");
            } else {
                m.put("formName", "-");
            }

            // 步骤摘要
            try {
                List<?> steps = JSONUtil.parseArray(t.getStepChain()).toList(Map.class);
                List<String> stepNames = steps.stream()
                        .map(s -> ((Map<?, ?>) s).get("name").toString())
                        .toList();
                m.put("stepSummary", String.join(" → ", stepNames));
            } catch (Exception e) {
                m.put("stepSummary", "-");
            }

            return m;
        });

        return Result.success(enriched);
    }

    @GetMapping("/{id}")
    @RequiresPermission("approval:template:list")
    @Operation(summary = "查询审批模板详情")
    public Result<SysProcessTemplate> getById(@PathVariable Long id) {
        SysProcessTemplate template = templateService.getById(id);
        return template != null ? Result.success(template) : Result.error(ResultCode.DATA_NOT_EXIST);
    }

    @PostMapping
    @RequiresPermission("approval:template:add")
    @Operation(summary = "新增审批模板")
    public Result<SysProcessTemplate> create(@Valid @RequestBody ProcessTemplateDTO dto) {
        SysProcessTemplate template = new SysProcessTemplate();
        template.setTemplateName(dto.getTemplateName());
        template.setDescription(dto.getDescription());
        template.setDeptId(dto.getDeptId());
        template.setFormId(dto.getFormId());
        template.setStepChain(JSONUtil.toJsonStr(dto.getStepChain()));
        template.setStatus(dto.getStatus() != null ? dto.getStatus() : 0);
        templateService.save(template);
        return Result.success(template);
    }

    @PutMapping("/{id}")
    @RequiresPermission("approval:template:edit")
    @Operation(summary = "修改审批模板")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProcessTemplateDTO dto) {
        SysProcessTemplate template = templateService.getById(id);
        if (template == null) return Result.error(ResultCode.DATA_NOT_EXIST);

        template.setTemplateName(dto.getTemplateName());
        template.setDescription(dto.getDescription());
        template.setDeptId(dto.getDeptId());
        template.setFormId(dto.getFormId());
        template.setStepChain(JSONUtil.toJsonStr(dto.getStepChain()));
        template.setStatus(dto.getStatus() != null ? dto.getStatus() : 0);
        templateService.updateById(template);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("approval:template:del")
    @Operation(summary = "删除审批模板")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.removeById(id);
        return Result.success();
    }
}
