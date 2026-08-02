package com.jinfu.form.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinfu.form.entity.FormFieldPermission;
import com.jinfu.form.entity.FormFieldPermission.FieldPermItem;
import com.jinfu.form.mapper.FormFieldPermissionMapper;
import com.jinfu.form.service.FormFieldPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FormFieldPermissionServiceImpl
        extends ServiceImpl<FormFieldPermissionMapper, FormFieldPermission>
        implements FormFieldPermissionService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePermissions(Long formId, String procDefId, String nodeId,
                                List<FieldPermItem> items) {
        // 删除该表单+节点已有权限
        this.remove(new LambdaQueryWrapper<FormFieldPermission>()
                .eq(FormFieldPermission::getFormId, formId)
                .eq(FormFieldPermission::getNodeId, nodeId));

        // 保存新权限
        if (items != null && !items.isEmpty()) {
            List<FormFieldPermission> entities = new ArrayList<>();
            for (FieldPermItem item : items) {
                FormFieldPermission perm = new FormFieldPermission();
                perm.setFormId(formId);
                perm.setProcDefId(procDefId);
                perm.setNodeId(nodeId);
                perm.setFieldKey(item.getFieldKey());
                perm.setPermission(item.getPermission());
                perm.setCreateTime(LocalDateTime.now());
                entities.add(perm);
            }
            this.saveBatch(entities);
        }
    }

    @Override
    public List<FormFieldPermission> getByFormAndNode(Long formId, String nodeId) {
        return this.list(new LambdaQueryWrapper<FormFieldPermission>()
                .eq(FormFieldPermission::getFormId, formId)
                .eq(FormFieldPermission::getNodeId, nodeId));
    }

    @Override
    public List<FormFieldPermission> getByFormId(Long formId) {
        return this.list(new LambdaQueryWrapper<FormFieldPermission>()
                .eq(FormFieldPermission::getFormId, formId)
                .orderByAsc(FormFieldPermission::getNodeId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByFormId(Long formId) {
        this.remove(new LambdaQueryWrapper<FormFieldPermission>()
                .eq(FormFieldPermission::getFormId, formId));
    }
}
