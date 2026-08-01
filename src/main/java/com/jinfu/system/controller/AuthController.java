package com.jinfu.system.controller;

import com.jinfu.common.constant.SecurityConstants;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.Result;
import com.jinfu.common.result.ResultCode;
import com.jinfu.form.mapper.FormDefinitionMapper;
import com.jinfu.security.entity.LoginUser;
import com.jinfu.security.service.LoginAttemptService;
import com.jinfu.security.service.TokenService;
import com.jinfu.system.entity.SysMenu;
import com.jinfu.system.entity.SysUser;
import com.jinfu.system.mapper.SysMenuMapper;
import com.jinfu.system.mapper.SysRoleMapper;
import com.jinfu.system.service.SysMenuService;
import com.jinfu.system.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/system/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login / Logout / User Info")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final LoginAttemptService loginAttemptService;
    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;
    private final SysMenuService sysMenuService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final FormDefinitionMapper formDefinitionMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    // ==================== Dashboard Stats ====================

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> result = new HashMap<>();
        result.put("userCount", sysUserMapper.selectCount(null));
        result.put("todoCount", taskService.createTaskQuery().count());
        result.put("processCount", repositoryService.createProcessDefinitionQuery().count());
        result.put("formCount", formDefinitionMapper.selectCount(null));
        return Result.success(result);
    }

    // ==================== Login ====================

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Username + password login, returns JWT token")
    public Result<Map<String, Object>> login(@RequestBody @Valid LoginRequest loginRequest) {
        String username = loginRequest.getUsername();

        // Brute-force protection: locked accounts are rejected before authentication
        if (loginAttemptService.isLocked(username)) {
            long minutes = loginAttemptService.getLockRemainingMinutes(username);
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(),
                    "登录失败次数过多，账号已锁定，请 " + minutes + " 分钟后再试");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            username,
                            loginRequest.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            loginAttemptService.onLoginFail(username);
            throw e;
        }
        loginAttemptService.onLoginSuccess(username);

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        String token = tokenService.createToken(loginUser);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", loginUser.getUserId());
        result.put("username", loginUser.getUsername());
        result.put("nickname", loginUser.getNickname());

        return Result.success("Login successful", result);
    }

    // ==================== Logout ====================

    @PostMapping("/logout")
    @Operation(summary = "Logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader(SecurityConstants.TOKEN_HEADER);
        tokenService.logout(token);
        return Result.success();
    }

    // ==================== Get Current User Info ====================

    @GetMapping("/info")
    @Operation(summary = "Get current logged-in user info + roles + permissions")
    public Result<Map<String, Object>> info(Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        Map<String, Object> info = new HashMap<>();
        info.put("userId", loginUser.getUserId());
        info.put("username", loginUser.getUsername());

        // Profile fields are loaded from DB — the Redis session cache holds
        // authorization data only (userId/username/status/roles/permissions)
        SysUser sysUser = sysUserMapper.selectById(loginUser.getUserId());
        if (sysUser != null) {
            info.put("nickname", sysUser.getNickname());
            info.put("avatar", sysUser.getAvatar());
            info.put("email", sysUser.getEmail());
            info.put("phone", sysUser.getPhone());
        }

        info.put("roles", loginUser.getRoles());
        info.put("permissions", loginUser.getPermissions());

        // Build menu tree
        List<SysMenu> flatMenus = sysMenuMapper.selectMenuTreeByUserId(loginUser.getUserId());
        List<SysMenu> menus = sysMenuService.buildMenuTree(flatMenus);
        info.put("menus", menus);

        return Result.success(info);
    }

    // ==================== Login Request DTO ====================

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;
    }
}
