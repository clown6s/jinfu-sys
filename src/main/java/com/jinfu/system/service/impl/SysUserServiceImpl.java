package com.jinfu.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.ResultCode;
import com.jinfu.system.dto.SysUserDTO;
import com.jinfu.system.entity.SysUser;
import com.jinfu.system.entity.SysUserRole;
import com.jinfu.system.mapper.SysUserMapper;
import com.jinfu.system.mapper.SysUserRoleMapper;
import com.jinfu.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final PasswordEncoder passwordEncoder;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public IPage<SysUser> selectPage(Page<SysUser> page, SysUser user) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(user.getUsername()), SysUser::getUsername, user.getUsername())
                .like(StringUtils.hasText(user.getPhone()), SysUser::getPhone, user.getPhone())
                .eq(user.getStatus() != null, SysUser::getStatus, user.getStatus())
                .eq(user.getDeptId() != null, SysUser::getDeptId, user.getDeptId())
                .orderByAsc(SysUser::getId);
        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertUser(SysUser user) {
        if (this.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername())) > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "Username already exists: " + user.getUsername());
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        this.save(user);

        List<Long> roleIds = getRoleIds(user);
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(SysUser user) {
        SysUser existing = this.getById(user.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "User does not exist: " + user.getId());
        }
        if (!existing.getUsername().equals(user.getUsername())
                && this.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername())) > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "Username already exists: " + user.getUsername());
        }
        user.setPassword(null);
        this.updateById(user);

        List<Long> roleIds = getRoleIds(user);
        if (roleIds != null) {
            sysUserRoleMapper.delete(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        this.removeById(userId);
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "User does not exist: " + userId);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
    }

    @Override
    public void changeStatus(Long userId, Integer status) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "User does not exist: " + userId);
        }
        user.setStatus(status);
        this.updateById(user);
    }

    @Override
    public SysUser selectUserById(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "User does not exist: " + userId);
        }
        return user;
    }

    private List<Long> getRoleIds(SysUser user) {
        if (user instanceof SysUserDTO dto) {
            return dto.getRoleIds();
        }
        return null;
    }
}
