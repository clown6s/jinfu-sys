package com.jinfu.form.service;

import com.jinfu.form.entity.FormInstance;

public interface FormInstanceService {

    FormInstance getById(Long id);

    FormInstance getByProcInstId(String procInstId);

    Long save(Long formInstanceId, String formKey, String procInstId,
              String title, Long creator, String businessDataJson);

    void updateBusinessData(Long id, String businessDataJson);

    void bindProcInst(Long id, String procInstId);
}
