package com.jinfu.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.ResultCode;
import com.jinfu.system.entity.SysDept;
import com.jinfu.system.entity.SysUser;
import com.jinfu.system.mapper.SysDeptMapper;
import com.jinfu.system.mapper.SysUserMapper;
import com.jinfu.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {

    private final SysUserMapper sysUserMapper;

    @Override
    public List<SysDept> selectDeptTree(SysDept dept) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(dept.getDeptName()), SysDept::getDeptName, dept.getDeptName())
                .eq(dept.getStatus() != null, SysDept::getStatus, dept.getStatus())
                .orderByAsc(SysDept::getSort);
        List<SysDept> all = this.list(wrapper);
        return buildTree(all);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertDept(SysDept dept) {
        if (dept.getParentId() != null && dept.getParentId() != 0) {
            SysDept parent = this.getById(dept.getParentId());
            if (parent == null) {
                throw new BusinessException(ResultCode.DATA_NOT_EXIST, "Parent department does not exist: " + dept.getParentId());
            }
        }
        this.save(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(SysDept dept) {
        SysDept existing = this.getById(dept.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "Department does not exist: " + dept.getId());
        }
        if (dept.getParentId() != null && dept.getParentId().equals(dept.getId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Cannot set self as parent department");
        }
        this.updateById(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDept(Long deptId) {
        if (lambdaQuery().eq(SysDept::getParentId, deptId).exists()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Department has sub-departments, cannot delete");
        }
        if (sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, deptId)) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Department has assigned users, cannot delete");
        }
        this.removeById(deptId);
    }

    private List<SysDept> buildTree(List<SysDept> all) {
        Map<Long, List<SysDept>> parentMap = all.stream()
                .filter(d -> d.getParentId() != null && d.getParentId() != 0)
                .collect(Collectors.groupingBy(SysDept::getParentId));

        List<SysDept> roots = new ArrayList<>();
        for (SysDept dept : all) {
            Long parentId = dept.getParentId();
            if (parentId == null || parentId == 0) {
                roots.add(dept);
            }
            List<SysDept> children = parentMap.get(dept.getId());
            if (children != null) {
                dept.setChildren(children);
            }
        }
        return roots;
    }
}
