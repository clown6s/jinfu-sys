package com.jinfu.system.dto;

import com.jinfu.system.entity.SysRole;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleDTO extends SysRole {

    /**
     * Menu IDs assigned to the role, used on create/update.
     */
    private List<Long> menuIds;
}
