package com.jinfu.flowable.controller;

import com.jinfu.common.result.Result;
import com.jinfu.security.annotation.RequiresPermission;
import com.jinfu.system.entity.SysMenu;
import com.jinfu.system.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
@Tag(name = "Menu Management", description = "Menu CRUD and tree building")
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @GetMapping("/list")
    @RequiresPermission("system:menu:list")
    @Operation(summary = "Flat menu list", description = "Query menus with filters (menuName, menuType, status)")
    public Result<List<SysMenu>> list(SysMenu menu) {
        return Result.success(sysMenuService.selectMenuList(menu));
    }

    @GetMapping("/tree")
    @RequiresPermission("system:menu:list")
    @Operation(summary = "Menu tree", description = "Tree structure of all menus (M and C type only)")
    public Result<List<SysMenu>> tree() {
        return Result.success(sysMenuService.selectMenuTree());
    }

    @GetMapping("/{menuId}")
    @RequiresPermission("system:menu:list")
    @Operation(summary = "Get menu by ID")
    public Result<SysMenu> getInfo(
            @Parameter(description = "Menu ID") @PathVariable Long menuId) {
        return Result.success(sysMenuService.selectMenuById(menuId));
    }

    @PostMapping
    @RequiresPermission("system:menu:add")
    @Operation(summary = "Add menu")
    public Result<Void> add(@Valid @RequestBody SysMenu menu) {
        sysMenuService.insertMenu(menu);
        return Result.success();
    }

    @PutMapping
    @RequiresPermission("system:menu:edit")
    @Operation(summary = "Update menu", description = "Cannot set the menu as its own parent")
    public Result<Void> edit(@Valid @RequestBody SysMenu menu) {
        sysMenuService.updateMenu(menu);
        return Result.success();
    }

    @DeleteMapping("/{menuId}")
    @RequiresPermission("system:menu:del")
    @Operation(summary = "Delete menu", description = "Fails if the menu has child menus")
    public Result<Void> remove(
            @Parameter(description = "Menu ID") @PathVariable Long menuId) {
        sysMenuService.deleteMenu(menuId);
        return Result.success();
    }
}
