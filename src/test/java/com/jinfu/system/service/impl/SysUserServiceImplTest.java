package com.jinfu.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.ResultCode;
import com.jinfu.system.dto.SysUserDTO;
import com.jinfu.system.entity.SysUser;
import com.jinfu.system.entity.SysUserRole;
import com.jinfu.system.mapper.SysUserMapper;
import com.jinfu.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysUserServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    private SysUserServiceImpl sysUserService;

    @BeforeEach
    void setUp() throws Exception {
        sysUserService = new SysUserServiceImpl(passwordEncoder, sysUserRoleMapper);
        java.lang.reflect.Field baseMapperField =
                CrudRepository.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(sysUserService, sysUserMapper);
    }

    private static SysUser createUser(Long id, String username, String phone, String password) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setUsername(username);
        u.setNickname(username);
        u.setPhone(phone);
        u.setPassword(password);
        u.setStatus(0);
        u.setDeptId(1L);
        return u;
    }

    // ======================== selectPage ========================

    @Test
    void selectPageWithFiltersShouldReturnPagedResults() {
        Page<SysUser> page = new Page<>(1, 10);
        SysUser filter = createUser(null, "admin", null, null);
        List<SysUser> users = List.of(createUser(1L, "admin", "13800138000", null));

        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Page<SysUser> p = inv.getArgument(0);
            p.setRecords(users);
            p.setTotal(1);
            return p;
        }).when(sysUserMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        IPage<SysUser> result = sysUserService.selectPage(page, filter);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    void selectPageWithNoFiltersShouldReturnAll() {
        Page<SysUser> page = new Page<>(1, 10);

        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Page<SysUser> p = inv.getArgument(0);
            p.setRecords(List.of());
            p.setTotal(0);
            return p;
        }).when(sysUserMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        IPage<SysUser> result = sysUserService.selectPage(page, new SysUser());

        assertThat(result.getRecords()).isEmpty();
    }

    // ======================== insertUser ========================

    @Test
    void insertUserShouldSucceed() {
        SysUser user = createUser(1L, "newuser", "13900139000", "123456");

        doReturn(0L).when(sysUserMapper).selectCount(any(LambdaQueryWrapper.class));
        doReturn("encoded_pass").when(passwordEncoder).encode("123456");
        doReturn(1).when(sysUserMapper).insert(any(SysUser.class));

        sysUserService.insertUser(user);

        verify(sysUserMapper).insert(any(SysUser.class));
        verify(passwordEncoder).encode("123456");
        assertThat(user.getPassword()).isEqualTo("encoded_pass");
    }

    @Test
    void insertUserWithRoleIdsShouldAssignRoles() {
        SysUserDTO dto = new SysUserDTO();
        dto.setId(1L);
        dto.setUsername("newuser");
        dto.setPassword("123456");
        dto.setNickname("newuser");
        dto.setRoleIds(List.of(10L, 20L));

        doReturn(0L).when(sysUserMapper).selectCount(any(LambdaQueryWrapper.class));
        doReturn("encoded_pass").when(passwordEncoder).encode("123456");
        doReturn(1).when(sysUserMapper).insert(any(SysUser.class));
        doReturn(1).when(sysUserRoleMapper).insert(any(SysUserRole.class));

        sysUserService.insertUser(dto);

        verify(sysUserRoleMapper, times(2)).insert(any(SysUserRole.class));
    }

    @Test
    void insertUserWithDuplicateUsernameShouldThrow() {
        SysUser user = createUser(1L, "existing", null, "123456");

        doReturn(1L).when(sysUserMapper).selectCount(any(LambdaQueryWrapper.class));

        assertThatThrownBy(() -> sysUserService.insertUser(user))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Username already exists")
                .extracting("code")
                .isEqualTo(ResultCode.DUPLICATE_KEY.getCode());

        verify(sysUserMapper, never()).insert(any(SysUser.class));
        verify(passwordEncoder, never()).encode(any());
    }

    // ======================== updateUser ========================

    @Test
    void updateUserShouldSucceed() {
        SysUser user = createUser(1L, "sameuser", "13900139000", null);
        SysUser existing = createUser(1L, "sameuser", "13800138000", "old");

        doReturn(existing).when(sysUserMapper).selectById(1L);
        doReturn(1).when(sysUserMapper).updateById(any(SysUser.class));

        sysUserService.updateUser(user);

        verify(sysUserMapper).updateById(any(SysUser.class));
        assertThat(user.getPassword()).isNull();
    }

    @Test
    void updateUserNotFoundShouldThrow() {
        SysUser user = createUser(99L, "ghost", null, null);

        doReturn(null).when(sysUserMapper).selectById(99L);

        assertThatThrownBy(() -> sysUserService.updateUser(user))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User does not exist")
                .extracting("code")
                .isEqualTo(ResultCode.DATA_NOT_EXIST.getCode());

        verify(sysUserMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void updateUserWithChangedDuplicateUsernameShouldThrow() {
        SysUser user = createUser(1L, "taken", null, null);
        SysUser existing = createUser(1L, "original", null, "old");

        doReturn(existing).when(sysUserMapper).selectById(1L);
        doReturn(1L).when(sysUserMapper).selectCount(any(LambdaQueryWrapper.class));

        assertThatThrownBy(() -> sysUserService.updateUser(user))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Username already exists");

        verify(sysUserMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void updateUserWithRoleIdsShouldReplaceRoles() {
        SysUserDTO dto = new SysUserDTO();
        dto.setId(1L);
        dto.setUsername("sameuser");
        dto.setNickname("sameuser");
        dto.setRoleIds(List.of(30L, 40L, 50L));
        SysUser existing = createUser(1L, "sameuser", null, "old");

        doReturn(existing).when(sysUserMapper).selectById(1L);
        doReturn(1).when(sysUserMapper).updateById(any(SysUser.class));
        doReturn(3).when(sysUserRoleMapper).delete(any(LambdaQueryWrapper.class));
        doReturn(1).when(sysUserRoleMapper).insert(any(SysUserRole.class));

        sysUserService.updateUser(dto);

        verify(sysUserRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(sysUserRoleMapper, times(3)).insert(any(SysUserRole.class));
    }

    // ======================== deleteUser ========================

    @Test
    void deleteUserShouldRemoveUserAndRoles() {
        doReturn(1).when(sysUserRoleMapper).delete(any(LambdaQueryWrapper.class));
        doReturn(1).when(sysUserMapper).deleteById(1L);

        sysUserService.deleteUser(1L);

        verify(sysUserRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(sysUserMapper).deleteById(1L);
    }

    @Test
    void deleteUserWithNoRolesShouldStillSucceed() {
        doReturn(0).when(sysUserRoleMapper).delete(any(LambdaQueryWrapper.class));
        doReturn(1).when(sysUserMapper).deleteById(2L);

        sysUserService.deleteUser(2L);

        verify(sysUserRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(sysUserMapper).deleteById(2L);
    }

    // ======================== resetPassword ========================

    @Test
    void resetPasswordShouldEncodeAndUpdate() {
        SysUser user = createUser(1L, "test", null, "old_encoded");

        doReturn(user).when(sysUserMapper).selectById(1L);
        doReturn("new_encoded").when(passwordEncoder).encode("newpass");
        doReturn(1).when(sysUserMapper).updateById(any(SysUser.class));

        sysUserService.resetPassword(1L, "newpass");

        assertThat(user.getPassword()).isEqualTo("new_encoded");
        verify(passwordEncoder).encode("newpass");
    }

    @Test
    void resetPasswordForNonExistingUserShouldThrow() {
        doReturn(null).when(sysUserMapper).selectById(99L);

        assertThatThrownBy(() -> sysUserService.resetPassword(99L, "newpass"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User does not exist");

        verify(passwordEncoder, never()).encode(any());
        verify(sysUserMapper, never()).updateById(any(SysUser.class));
    }

    // ======================== changeStatus ========================

    @Test
    void changeStatusShouldUpdateUserStatus() {
        SysUser user = createUser(1L, "test", null, null);
        user.setStatus(0);

        doReturn(user).when(sysUserMapper).selectById(1L);
        doReturn(1).when(sysUserMapper).updateById(any(SysUser.class));

        sysUserService.changeStatus(1L, 1);

        assertThat(user.getStatus()).isEqualTo(1);
    }

    @Test
    void changeStatusForNonExistingUserShouldThrow() {
        doReturn(null).when(sysUserMapper).selectById(99L);

        assertThatThrownBy(() -> sysUserService.changeStatus(99L, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User does not exist");

        verify(sysUserMapper, never()).updateById(any(SysUser.class));
    }

    // ======================== selectUserById ========================

    @Test
    void selectUserByIdShouldReturnUser() {
        SysUser user = createUser(1L, "admin", "13800138000", null);
        doReturn(user).when(sysUserMapper).selectById(1L);

        SysUser result = sysUserService.selectUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("admin");
    }

    @Test
    void selectUserByIdNotFoundShouldThrow() {
        doReturn(null).when(sysUserMapper).selectById(99L);

        assertThatThrownBy(() -> sysUserService.selectUserById(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User does not exist");
    }
}
