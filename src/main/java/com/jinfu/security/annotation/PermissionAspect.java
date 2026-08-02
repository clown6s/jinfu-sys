package com.jinfu.security.annotation;

import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.ResultCode;
import com.jinfu.security.entity.LoginUser;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(com.jinfu.security.annotation.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);

        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // admin 绕过所有权限校验
        Set<String> roles = loginUser.getRoles();
        if (roles != null && roles.contains("admin")) {
            return joinPoint.proceed();
        }

        Set<String> permissions = loginUser.getPermissions();
        if (permissions == null || permissions.isEmpty()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        String requiredPerm = annotation.value();
        if (!permissions.contains(requiredPerm)) {
            throw new BusinessException(ResultCode.FORBIDDEN,
                    "Missing permission: " + requiredPerm);
        }

        return joinPoint.proceed();
    }

    @Around("@annotation(com.jinfu.security.annotation.RequiresRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresRole annotation = method.getAnnotation(RequiresRole.class);

        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        Set<String> roles = loginUser.getRoles();
        if (roles == null || !roles.contains(annotation.value())) {
            throw new BusinessException(ResultCode.FORBIDDEN,
                    "Missing role: " + annotation.value());
        }

        return joinPoint.proceed();
    }

    private LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            return null;
        }
        return (LoginUser) authentication.getPrincipal();
    }
}
