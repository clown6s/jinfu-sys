package com.jinfu.security.service;

import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.ResultCode;
import com.jinfu.security.entity.LoginUser;
import com.jinfu.system.entity.SysUser;
import com.jinfu.system.mapper.SysMenuMapper;
import com.jinfu.system.mapper.SysRoleMapper;
import com.jinfu.system.mapper.SysUserMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        LoginUser loginUser = LoginUser.from(user);

        // 加载角色
        Set<String> roles = sysRoleMapper.selectRoleKeysByUserId(user.getId());
        loginUser.setRoles(roles);

        // 加载权限
        Set<String> permissions = sysMenuMapper.selectPermsByUserId(user.getId());
        loginUser.setPermissions(permissions);

        return loginUser;
    }
}
