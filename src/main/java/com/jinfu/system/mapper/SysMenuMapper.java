package com.jinfu.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinfu.system.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    Set<String> selectPermsByUserId(@Param("userId") Long userId);

    List<SysMenu> selectMenuTreeByUserId(@Param("userId") Long userId);

    List<SysMenu> selectAllMenuTree();
}
