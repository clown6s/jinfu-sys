package com.jinfu.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jinfu.system.entity.SysDept;

import java.util.List;

public interface SysDeptService extends IService<SysDept> {

    List<SysDept> selectDeptTree(SysDept dept);

    void insertDept(SysDept dept);

    void updateDept(SysDept dept);

    void deleteDept(Long deptId);
}
