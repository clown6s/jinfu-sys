package com.jinfu.form.service;

import com.jinfu.form.entity.FormFieldPermission;
import com.jinfu.form.entity.FormFieldPermission.FieldPermItem;

import java.util.List;

public interface FormFieldPermissionService {

    /** Save field permissions for a single node. Deletes old ones before saving. */
    void savePermissions(Long formId, String procDefId, String nodeId, List<FieldPermItem> items);

    /** Get field permissions for a node of a form */
    List<FormFieldPermission> getByFormAndNode(Long formId, String nodeId);

    /** Get all field permissions for a form (grouped by node) */
    List<FormFieldPermission> getByFormId(Long formId);

    /** Delete all field permissions for a form */
    void deleteByFormId(Long formId);
}
