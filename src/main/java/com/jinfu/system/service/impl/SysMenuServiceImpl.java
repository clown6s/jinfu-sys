package com.jinfu.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.ResultCode;
import com.jinfu.system.entity.SysMenu;
import com.jinfu.system.mapper.SysMenuMapper;
import com.jinfu.system.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    private final SysMenuMapper sysMenuMapper;

    @Override
    public List<SysMenu> selectMenuList(SysMenu menu) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(menu.getMenuName()), SysMenu::getMenuName, menu.getMenuName())
                .eq(StringUtils.hasText(menu.getMenuType()), SysMenu::getMenuType, menu.getMenuType())
                .eq(menu.getStatus() != null, SysMenu::getStatus, menu.getStatus())
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getSort);
        return this.list(wrapper);
    }

    @Override
    public List<SysMenu> selectMenuTree() {
        List<SysMenu> allMenus = sysMenuMapper.selectAllMenuTree();
        return buildMenuTree(allMenus);
    }

    @Override
    public List<SysMenu> buildMenuTree(List<SysMenu> menus) {
        if (menus == null || menus.isEmpty()) {
            return List.of();
        }
        // 按排序号排序，保证渲染顺序一致
        menus.sort(Comparator.comparing(SysMenu::getSort, Comparator.nullsLast(Comparator.naturalOrder())));

        // 找出根节点（parentId 为 null 或 0）
        List<SysMenu> roots = menus.stream()
                .filter(m -> m.getParentId() == null || m.getParentId() == 0)
                .collect(Collectors.toList());

        for (SysMenu root : roots) {
            root.setChildren(getChildren(root.getId(), menus));
        }
        return roots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertMenu(SysMenu menu) {
        // 重复性校验
        checkDuplicate(menu, null);

        // 设置默认值
        if (menu.getVisible() == null) {
            menu.setVisible(0);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(0);
        }
        this.save(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(SysMenu menu) {
        // 存在性校验
        SysMenu existing = this.getById(menu.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST,
                    "Menu not found: " + menu.getId());
        }

        // 禁止将自身设为父节点
        if (menu.getParentId() != null && menu.getParentId().equals(menu.getId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "A menu cannot be its own parent");
        }

        // 重复性校验（排除当前记录）
        checkDuplicate(menu, menu.getId());

        this.updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long menuId) {
        // 校验无子节点
        long childCount = this.count(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, menuId));
        if (childCount > 0) {
            throw new BusinessException(ResultCode.ILLEGAL_OPERATION,
                    "Menu has child menus, cannot delete");
        }
        this.removeById(menuId);
    }

    @Override
    public SysMenu selectMenuById(Long menuId) {
        SysMenu menu = this.getById(menuId);
        if (menu == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST,
                    "Menu not found: " + menuId);
        }
        return menu;
    }

    @Override
    public List<Long> selectAllMenuIds() {
        return this.list().stream()
                .map(SysMenu::getId)
                .collect(Collectors.toList());
    }

    // ---- Private helpers ----

    /**
     * Check duplicate menu name and permission code within the same parent.
     */
    private void checkDuplicate(SysMenu menu, Long excludeId) {
        LambdaQueryWrapper<SysMenu> nameWrapper = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getMenuName, menu.getMenuName())
                .eq(SysMenu::getParentId,
                        menu.getParentId() != null ? menu.getParentId() : 0);
        if (excludeId != null) {
            nameWrapper.ne(SysMenu::getId, excludeId);
        }
        if (this.count(nameWrapper) > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY,
                    "Menu name already exists under this parent: " + menu.getMenuName());
        }

        if (StringUtils.hasText(menu.getPerms())) {
            LambdaQueryWrapper<SysMenu> permsWrapper = new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getPerms, menu.getPerms());
            if (excludeId != null) {
                permsWrapper.ne(SysMenu::getId, excludeId);
            }
            if (this.count(permsWrapper) > 0) {
                throw new BusinessException(ResultCode.DUPLICATE_KEY,
                        "Permission code already exists: " + menu.getPerms());
            }
        }
    }

    /**
     * Recursively get children of a menu node.
     */
    private List<SysMenu> getChildren(Long parentId, List<SysMenu> allMenus) {
        List<SysMenu> children = allMenus.stream()
                .filter(m -> Objects.equals(m.getParentId(), parentId))
                .collect(Collectors.toList());

        if (children.isEmpty()) {
            return null;
        }

        for (SysMenu child : children) {
            child.setChildren(getChildren(child.getId(), allMenus));
        }
        return children;
    }
}
