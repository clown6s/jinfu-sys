package com.jinfu.form.service;

import com.jinfu.form.entity.FormDefinition;

import java.util.List;

public interface FormDefinitionService {

    List<FormDefinition> list(String keyword, Integer status);

    FormDefinition getById(Long id);

    FormDefinition getByFormKey(String formKey);

    void insert(FormDefinition formDef);

    void update(FormDefinition formDef);

    void delete(Long id);

    void publish(Long id);

    void deprecate(Long id);
}
