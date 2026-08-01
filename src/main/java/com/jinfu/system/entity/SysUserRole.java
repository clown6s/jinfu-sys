package com.jinfu.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * User-Role many-to-many join table.
 * Composite PK (user_id, role_id) — MP doesn't support composite keys natively,
 * so userId is designated as @TableId for MP internal mapping. All queries use
 * LambdaQueryWrapper, never xxById methods.
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {

    @TableId(type = IdType.NONE)
    private Long userId;

    private Long roleId;
}
