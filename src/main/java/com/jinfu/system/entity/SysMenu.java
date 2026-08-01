package com.jinfu.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jinfu.common.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    private String path;
    private String component;
    private String perms;
    /**
     * Menu type: M=directory, C=menu, F=button
     */
    private String menuType;
    private String icon;
    private Integer sort;
    private Integer visible;
    private Integer status;

    @TableField(exist = false)
    private String parentName;

    /**
     * Children menus, used when building the menu tree (not persisted).
     */
    @TableField(exist = false)
    private List<SysMenu> children;
}
