package com.jinfu.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jinfu.system.entity.SysMenu;

import java.util.List;

public interface SysMenuService extends IService<SysMenu> {

    /**
     * Flat menu list with filters (menuName, status, menuType, etc.).
     */
    List<SysMenu> selectMenuList(SysMenu menu);

    /**
     * Tree structure of all menus (M and C type only).
     */
    List<SysMenu> selectMenuTree();

    /**
     * Build a menu tree from a flat list, recursively grouping by parentId.
     */
    List<SysMenu> buildMenuTree(List<SysMenu> menus);

    /**
     * Create a menu. Validates duplicate name/path/permission.
     */
    void insertMenu(SysMenu menu);

    /**
     * Update a menu. Checks that the menu is not set as its own parent.
     */
    void updateMenu(SysMenu menu);

    /**
     * Delete a menu. Fails if it has child menus.
     */
    void deleteMenu(Long menuId);

    /**
     * Get a single menu by its primary key.
     */
    SysMenu selectMenuById(Long menuId);

    /**
     * All menu IDs, used for super-admin role assignment.
     */
    List<Long> selectAllMenuIds();
}
