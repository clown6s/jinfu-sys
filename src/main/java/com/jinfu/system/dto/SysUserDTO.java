package com.jinfu.system.dto;

import com.jinfu.system.entity.SysUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserDTO extends SysUser {

    /**
     * Role IDs assigned to the user, used on create/update.
     */
    private List<Long> roleIds;
}
