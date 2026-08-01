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
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    private Integer sort;
    private String leader;
    private String phone;
    private String email;
    private Integer status;

    @TableField(exist = false)
    private List<SysDept> children;
}
