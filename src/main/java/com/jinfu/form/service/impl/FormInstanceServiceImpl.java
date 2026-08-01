package com.jinfu.form.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinfu.form.entity.FormInstance;
import com.jinfu.form.mapper.FormInstanceMapper;
import com.jinfu.form.service.FormInstanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FormInstanceServiceImpl
        extends ServiceImpl<FormInstanceMapper, FormInstance>
        implements FormInstanceService {

    @Override
    public FormInstance getById(Long id) {
        return super.getById(id);
    }

    @Override
    public FormInstance getByProcInstId(String procInstId) {
        return this.getOne(new LambdaQueryWrapper<FormInstance>()
                .eq(FormInstance::getProcInstId, procInstId)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(Long formInstanceId, String formKey, String procInstId,
                     String title, Long creator, String businessDataJson) {
        FormInstance instance;
        if (formInstanceId != null) {
            instance = super.getById(formInstanceId);
            if (instance == null) {
                instance = new FormInstance();
            }
        } else {
            instance = new FormInstance();
            instance.setCreateTime(LocalDateTime.now());
        }
        instance.setFormKey(formKey);
        instance.setProcInstId(procInstId);
        instance.setTitle(title);
        instance.setCreator(creator);
        instance.setBusinessDataJson(businessDataJson);
        instance.setUpdateTime(LocalDateTime.now());

        if (instance.getId() != null) {
            this.updateById(instance);
        } else {
            this.save(instance);
        }
        return instance.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBusinessData(Long id, String businessDataJson) {
        FormInstance instance = super.getById(id);
        if (instance != null) {
            instance.setBusinessDataJson(businessDataJson);
            instance.setUpdateTime(LocalDateTime.now());
            this.updateById(instance);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindProcInst(Long id, String procInstId) {
        FormInstance instance = super.getById(id);
        if (instance != null) {
            instance.setProcInstId(procInstId);
            instance.setUpdateTime(LocalDateTime.now());
            this.updateById(instance);
        }
    }
}
