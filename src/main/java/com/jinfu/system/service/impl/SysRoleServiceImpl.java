package com.jinfu.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.ResultCode;
import com.jinfu.system.dto.SysRoleDTO;
import com.jinfu.system.entity.SysRole;
import com.jinfu.system.entity.SysRoleMenu;
import com.jinfu.system.entity.SysUserRole;
import com.jinfu.system.mapper.SysRoleMapper;
import com.jinfu.system.mapper.SysRoleMenuMapper;
import com.jinfu.system.mapper.SysUserRoleMapper;
import com.jinfu.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public IPage<SysRole> selectPage(Page<SysRole> page, SysRole role) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(role.getRoleName()), SysRole::getRoleName, role.getRoleName())
                .like(StringUtils.hasText(role.getRoleKey()), SysRole::getRoleKey, role.getRoleKey())
                .eq(role.getStatus() != null, SysRole::getStatus, role.getStatus())
                .orderByAsc(SysRole::getSort);
        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertRole(SysRoleDTO roleDTO) {
        // Duplicate role key check
        boolean exists = this.count(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, roleDTO.getRoleKey())) > 0;
        if (exists) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY,
                    "Role key already exists: " + roleDTO.getRoleKey());
        }
        this.save(roleDTO);
        insertRoleMenu(roleDTO.getId(), roleDTO.getMenuIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(SysRoleDTO roleDTO) {
        SysRole existing = this.getById(roleDTO.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST,
                    "Role not found: " + roleDTO.getId());
        }
        // Duplicate role key check (excluding current id)
        SysRole duplicate = this.getOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, roleDTO.getRoleKey())
                .ne(SysRole::getId, roleDTO.getId()));
        if (duplicate != null) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY,
                    "Role key already exists: " + roleDTO.getRoleKey());
        }
        this.updateById(roleDTO);
        // Delete old role-menu relations, then insert new ones
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleDTO.getId()));
        insertRoleMenu(roleDTO.getId(), roleDTO.getMenuIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        // Check no users assigned to the role
        long userCount = sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, roleId));
        if (userCount > 0) {
            throw new BusinessException(ResultCode.ILLEGAL_OPERATION,
                    "Role has assigned users, cannot delete");
        }
        this.removeById(roleId);
        // Delete role-menu relations
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId));
    }

    @Override
    public List<SysRole> selectRoleAll() {
        return this.list(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 0)
                .orderByAsc(SysRole::getSort));
    }

    @Override
    public List<Long> selectMenuIdsByRoleId(Long roleId) {
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId));
        return roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    /**
     * Insert role-menu relations in batch.
     */
    private void insertRoleMenu(Long roleId, List<Long> menuIds) {
        if (CollectionUtils.isEmpty(menuIds)) {
            return;
        }
        for (Long menuId : menuIds) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            sysRoleMenuMapper.insert(roleMenu);
        }
    }
}
