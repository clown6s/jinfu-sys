package com.jinfu.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinfu.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    Set<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);
}
