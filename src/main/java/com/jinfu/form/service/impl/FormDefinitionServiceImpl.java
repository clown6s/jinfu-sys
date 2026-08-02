package com.jinfu.form.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.ResultCode;
import com.jinfu.form.entity.FormDefinition;
import com.jinfu.form.mapper.FormDefinitionMapper;
import com.jinfu.form.service.FormDefinitionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class FormDefinitionServiceImpl
        extends ServiceImpl<FormDefinitionMapper, FormDefinition>
        implements FormDefinitionService {

    @Override
    public List<FormDefinition> list(String keyword, Integer status) {
        LambdaQueryWrapper<FormDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), FormDefinition::getName, keyword)
               .eq(status != null, FormDefinition::getStatus, status)
               .orderByDesc(FormDefinition::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public FormDefinition getById(Long id) {
        FormDefinition formDef = super.getById(id);
        if (formDef == null) {
            throw new BusinessException(ResultCode.FORM_DEF_NOT_FOUND,
                    "Form definition not found: " + id);
        }
        return formDef;
    }

    @Override
    public FormDefinition getByFormKey(String formKey) {
        return lambdaQuery()
                .eq(FormDefinition::getFormKey, formKey)
                .orderByDesc(FormDefinition::getVersion)
                .last("LIMIT 1")
                .one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insert(FormDefinition formDef) {
        if (lambdaQuery().eq(FormDefinition::getFormKey, formDef.getFormKey()).exists()) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY,
                    "Form key already exists: " + formDef.getFormKey());
        }
        formDef.setVersion(1);
        formDef.setStatus(0);
        this.save(formDef);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(FormDefinition formDef) {
        FormDefinition existing = getById(formDef.getId());
        // 若 key 变更，校验唯一性
        if (StringUtils.hasText(formDef.getFormKey())
                && !formDef.getFormKey().equals(existing.getFormKey())) {
            if (lambdaQuery().eq(FormDefinition::getFormKey, formDef.getFormKey()).exists()) {
                throw new BusinessException(ResultCode.DUPLICATE_KEY,
                        "Form key already exists: " + formDef.getFormKey());
            }
        }
        // 若 schema 变更且表单已发布，版本号加一
        if (StringUtils.hasText(formDef.getSchemaJson())
                && existing.getStatus() == 1
                && !formDef.getSchemaJson().equals(existing.getSchemaJson())) {
            formDef.setVersion(existing.getVersion() + 1);
        }
        this.updateById(formDef);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getById(id);
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        FormDefinition formDef = getById(id);
        formDef.setStatus(1);
        this.updateById(formDef);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deprecate(Long id) {
        FormDefinition formDef = getById(id);
        formDef.setStatus(2);
        this.updateById(formDef);
    }
}
