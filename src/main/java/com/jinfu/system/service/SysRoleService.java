package com.jinfu.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinfu.system.dto.SysRoleDTO;
import com.jinfu.system.entity.SysRole;

import java.util.List;

public interface SysRoleService extends IService<SysRole> {

    /**
     * Paginated role query with filters (roleName, roleKey, status).
     */
    IPage<SysRole> selectPage(Page<SysRole> page, SysRole role);

    /**
     * Create a role together with its role-menu relations.
     */
    void insertRole(SysRoleDTO roleDTO);

    /**
     * Update a role: delete old role-menu relations then insert the new ones.
     */
    void updateRole(SysRoleDTO roleDTO);

    /**
     * Delete a role together with its role-menu relations.
     * Fails if any user is still assigned to the role.
     */
    void deleteRole(Long roleId);

    /**
     * All active roles (status = 0), sorted, for dropdowns.
     */
    List<SysRole> selectRoleAll();

    /**
     * Menu IDs assigned to the given role.
     */
    List<Long> selectMenuIdsByRoleId(Long roleId);
}
