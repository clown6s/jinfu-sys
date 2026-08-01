package com.jinfu.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.ResultCode;
import com.jinfu.system.dto.SysRoleDTO;
import com.jinfu.system.entity.SysRole;
import com.jinfu.system.entity.SysRoleMenu;
import com.jinfu.system.entity.SysUserRole;
import com.jinfu.system.mapper.SysRoleMapper;
import com.jinfu.system.mapper.SysRoleMenuMapper;
import com.jinfu.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysRoleServiceImplTest {

    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysRoleMenuMapper sysRoleMenuMapper;
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    private SysRoleServiceImpl sysRoleService;

    @BeforeEach
    void setUp() throws Exception {
        sysRoleService = new SysRoleServiceImpl(sysRoleMenuMapper, sysUserRoleMapper);
        java.lang.reflect.Field baseMapperField =
                CrudRepository.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(sysRoleService, sysRoleMapper);
    }

    private static SysRole createRole(Long id, String roleName, String roleKey, Integer sort, Integer status) {
        SysRole r = new SysRole();
        r.setId(id);
        r.setRoleName(roleName);
        r.setRoleKey(roleKey);
        r.setSort(sort);
        r.setStatus(status);
        return r;
    }

    private static SysRoleDTO createRoleDTO(Long id, String roleName, String roleKey, List<Long> menuIds) {
        SysRoleDTO dto = new SysRoleDTO();
        dto.setId(id);
        dto.setRoleName(roleName);
        dto.setRoleKey(roleKey);
        dto.setSort(1);
        dto.setStatus(0);
        dto.setMenuIds(menuIds);
        return dto;
    }

    // ======================== selectPage ========================

    @Test
    void selectPageWithFiltersShouldReturnResults() {
        Page<SysRole> page = new Page<>(1, 10);
        SysRole filter = new SysRole();
        filter.setRoleName("admin");

        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Page<SysRole> p = inv.getArgument(0);
            p.setRecords(List.of(createRole(1L, "Admin", "admin", 1, 0)));
            p.setTotal(1);
            return p;
        }).when(sysRoleMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        IPage<SysRole> result = sysRoleService.selectPage(page, filter);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getRoleKey()).isEqualTo("admin");
    }

    @Test
    void selectPageEmptyResult() {
        Page<SysRole> page = new Page<>(1, 10);

        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Page<SysRole> p = inv.getArgument(0);
            p.setRecords(List.of());
            p.setTotal(0);
            return p;
        }).when(sysRoleMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        IPage<SysRole> result = sysRoleService.selectPage(page, new SysRole());

        assertThat(result.getRecords()).isEmpty();
    }

    // ======================== insertRole ========================

    @Test
    void insertRoleShouldSucceed() {
        SysRoleDTO dto = createRoleDTO(1L, "Admin", "admin", List.of(1L, 2L, 3L));

        doReturn(0L).when(sysRoleMapper).selectCount(any(LambdaQueryWrapper.class));
        doReturn(1).when(sysRoleMapper).insert(any(SysRole.class));
        doReturn(1).when(sysRoleMenuMapper).insert(any(SysRoleMenu.class));

        sysRoleService.insertRole(dto);

        verify(sysRoleMapper).insert(any(SysRole.class));
        verify(sysRoleMenuMapper, times(3)).insert(any(SysRoleMenu.class));
    }

    @Test
    void insertRoleWithoutMenuIdsShouldStillSucceed() {
        SysRoleDTO dto = createRoleDTO(1L, "Viewer", "viewer", null);

        doReturn(0L).when(sysRoleMapper).selectCount(any(LambdaQueryWrapper.class));
        doReturn(1).when(sysRoleMapper).insert(any(SysRole.class));

        sysRoleService.insertRole(dto);

        verify(sysRoleMapper).insert(any(SysRole.class));
        verify(sysRoleMenuMapper, never()).insert(any(SysRoleMenu.class));
    }

    @Test
    void insertRoleWithDuplicateKeyShouldThrow() {
        SysRoleDTO dto = createRoleDTO(1L, "Admin", "admin", List.of(1L));

        doReturn(1L).when(sysRoleMapper).selectCount(any(LambdaQueryWrapper.class));

        assertThatThrownBy(() -> sysRoleService.insertRole(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Role key already exists")
                .extracting("code")
                .isEqualTo(ResultCode.DUPLICATE_KEY.getCode());

        verify(sysRoleMapper, never()).insert(any(SysRole.class));
    }

    // ======================== updateRole ========================

    @Test
    void updateRoleShouldSucceed() {
        SysRoleDTO dto = createRoleDTO(1L, "Admin Updated", "admin", List.of(4L, 5L));
        SysRole existing = createRole(1L, "Admin", "admin", 1, 0);

        doReturn(existing).when(sysRoleMapper).selectById(1L);
        doReturn(null).when(sysRoleMapper).selectOne(any(LambdaQueryWrapper.class), anyBoolean());
        doReturn(1).when(sysRoleMapper).updateById(any(SysRole.class));
        doReturn(2).when(sysRoleMenuMapper).delete(any(LambdaQueryWrapper.class));
        doReturn(1).when(sysRoleMenuMapper).insert(any(SysRoleMenu.class));

        sysRoleService.updateRole(dto);

        verify(sysRoleMapper).updateById(any(SysRole.class));
        verify(sysRoleMenuMapper).delete(any(LambdaQueryWrapper.class));
        verify(sysRoleMenuMapper, times(2)).insert(any(SysRoleMenu.class));
    }

    @Test
    void updateRoleNotFoundShouldThrow() {
        SysRoleDTO dto = createRoleDTO(99L, "Ghost", "ghost", null);

        doReturn(null).when(sysRoleMapper).selectById(99L);

        assertThatThrownBy(() -> sysRoleService.updateRole(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Role not found")
                .extracting("code")
                .isEqualTo(ResultCode.DATA_NOT_EXIST.getCode());

        verify(sysRoleMapper, never()).updateById(any(SysRole.class));
    }

    @Test
    void updateRoleWithDuplicateKeyShouldThrow() {
        SysRoleDTO dto = createRoleDTO(1L, "Admin", "taken", null);
        SysRole existing = createRole(1L, "Admin", "admin", 1, 0);
        SysRole duplicate = createRole(2L, "Other", "taken", 2, 0);

        doReturn(existing).when(sysRoleMapper).selectById(1L);
        doReturn(duplicate).when(sysRoleMapper).selectOne(any(LambdaQueryWrapper.class), anyBoolean());

        assertThatThrownBy(() -> sysRoleService.updateRole(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Role key already exists");

        verify(sysRoleMapper, never()).updateById(any(SysRole.class));
    }

    // ======================== deleteRole ========================

    @Test
    void deleteRoleShouldSucceed() {
        doReturn(0L).when(sysUserRoleMapper).selectCount(any(LambdaQueryWrapper.class));
        doReturn(1).when(sysRoleMapper).deleteById(1L);
        doReturn(3).when(sysRoleMenuMapper).delete(any(LambdaQueryWrapper.class));

        sysRoleService.deleteRole(1L);

        verify(sysRoleMapper).deleteById(1L);
        verify(sysRoleMenuMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void deleteRoleWithAssignedUsersShouldThrow() {
        doReturn(5L).when(sysUserRoleMapper).selectCount(any(LambdaQueryWrapper.class));

        assertThatThrownBy(() -> sysRoleService.deleteRole(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Role has assigned users")
                .extracting("code")
                .isEqualTo(ResultCode.ILLEGAL_OPERATION.getCode());

        verify(sysRoleMapper, never()).deleteById(anyLong());
        verify(sysRoleMenuMapper, never()).delete(any());
    }

    // ======================== selectRoleAll ========================

    @Test
    void selectRoleAllShouldReturnActiveRoles() {
        SysRole r1 = createRole(1L, "Admin", "admin", 1, 0);
        SysRole r2 = createRole(2L, "User", "user", 2, 0);

        doReturn(List.of(r1, r2)).when(sysRoleMapper).selectList(any(LambdaQueryWrapper.class));

        List<SysRole> roles = sysRoleService.selectRoleAll();

        assertThat(roles).hasSize(2);
        assertThat(roles).extracting("roleKey").containsExactly("admin", "user");
    }

    @Test
    void selectRoleAllEmptyResult() {
        doReturn(List.of()).when(sysRoleMapper).selectList(any(LambdaQueryWrapper.class));

        List<SysRole> roles = sysRoleService.selectRoleAll();

        assertThat(roles).isEmpty();
    }

    // ======================== selectMenuIdsByRoleId ========================

    @Test
    void selectMenuIdsByRoleIdShouldReturnMenuIds() {
        SysRoleMenu rm1 = new SysRoleMenu();
        rm1.setRoleId(1L);
        rm1.setMenuId(10L);
        SysRoleMenu rm2 = new SysRoleMenu();
        rm2.setRoleId(1L);
        rm2.setMenuId(20L);

        doReturn(List.of(rm1, rm2)).when(sysRoleMenuMapper).selectList(any(LambdaQueryWrapper.class));

        List<Long> menuIds = sysRoleService.selectMenuIdsByRoleId(1L);

        assertThat(menuIds).containsExactly(10L, 20L);
    }

    @Test
    void selectMenuIdsByRoleIdEmptyResult() {
        doReturn(List.of()).when(sysRoleMenuMapper).selectList(any(LambdaQueryWrapper.class));

        List<Long> menuIds = sysRoleService.selectMenuIdsByRoleId(1L);

        assertThat(menuIds).isEmpty();
    }
}
