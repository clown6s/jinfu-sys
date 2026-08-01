package com.jinfu.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jinfu.common.entity.BaseEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @Size(min = 6, max = 100, message = "密码长度需在6-100位之间")
    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    private String avatar;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private Long deptId;
    private Integer status;

    @TableField(exist = false)
    private String deptName;
}
