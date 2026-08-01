package com.jinfu.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinfu.system.entity.SysUser;

public interface SysUserService extends IService<SysUser> {

    IPage<SysUser> selectPage(Page<SysUser> page, SysUser user);

    void insertUser(SysUser user);

    void updateUser(SysUser user);

    void deleteUser(Long userId);

    void resetPassword(Long userId, String newPassword);

    void changeStatus(Long userId, Integer status);

    SysUser selectUserById(Long userId);
}
