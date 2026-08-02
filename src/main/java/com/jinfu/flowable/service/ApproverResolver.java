package com.jinfu.flowable.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.ResultCode;
import com.jinfu.system.entity.SysDept;
import com.jinfu.system.entity.SysRole;
import com.jinfu.system.entity.SysUser;
import com.jinfu.system.entity.SysUserRole;
import com.jinfu.system.mapper.SysDeptMapper;
import com.jinfu.system.mapper.SysRoleMapper;
import com.jinfu.system.mapper.SysUserMapper;
import com.jinfu.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 审批人动态解析 Bean — 供 BPMN 表达式调用：
 *   ${approverResolver.resolveDeptLeader(execution)}
 *   ${approverResolver.resolveRole(execution, 'admin')}
 *   ${approverResolver.resolveUser(execution, '123')}
 *
 * 返回值是 userId（字符串），赋给 UserTask 的 flowable:assignee。
 */
@Slf4j
@Component("approverResolver")
@RequiredArgsConstructor
public class ApproverResolver {

    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    /**
     * 解析部门负责人 → userId 字符串。
     * BPMN 表达式: ${approverResolver.resolveDeptLeader(execution)}
     * 从流程变量读取 initiatorId/deptId，查部门 leader 字段，再匹配用户。
     */
    public String resolveDeptLeader(DelegateExecution execution) {
        Long deptId = extractLongVar(execution, "deptId");
        if (deptId == null) {
            Long initiatorId = extractLongVar(execution, "initiatorId");
            if (initiatorId != null) {
                SysUser initiator = sysUserMapper.selectById(initiatorId);
                deptId = initiator != null ? initiator.getDeptId() : null;
            }
        }
        if (deptId == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "无法解析部门负责人：deptId 为空");
        }

        SysDept dept = sysDeptMapper.selectById(deptId);
        if (dept == null || StrUtil.isBlank(dept.getLeader())) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST,
                    "部门未设置负责人: deptId=" + deptId);
        }

        SysUser leader = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeptId, deptId)
                .eq(SysUser::getNickname, dept.getLeader())
                .last("LIMIT 1"));
        if (leader == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST,
                    "未找到部门负责人用户: " + dept.getLeader());
        }
        log.info("resolveDeptLeader: deptId={} -> leader userId={}", deptId, leader.getId());
        return String.valueOf(leader.getId());
    }

    /**
     * 按角色Key解析审批人 → 第一个匹配用户的 userId 字符串。
     * BPMN 表达式: ${approverResolver.resolveRole(execution, 'admin')}
     */
    public String resolveRole(DelegateExecution execution, String roleKey) {
        if (StrUtil.isBlank(roleKey)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "角色Key不能为空");
        }

        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, roleKey)
                .eq(SysRole::getStatus, 0));
        if (role == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "角色不存在或已停用: " + roleKey);
        }

        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, role.getId()));
        if (userRoles.isEmpty()) {
            log.warn("角色 {} 下没有用户", roleKey);
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "角色下无用户: " + roleKey);
        }

        SysUser user = sysUserMapper.selectById(userRoles.get(0).getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "角色用户不存在: " + roleKey);
        }
        log.info("resolveRole: roleKey={} -> userId={}", roleKey, user.getId());
        return String.valueOf(user.getId());
    }

    /**
     * 直接指定用户 → userId 字符串。
     * BPMN 表达式: ${approverResolver.resolveUser(execution, '123')}
     */
    public String resolveUser(DelegateExecution execution, String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "用户ID不能为空");
        }
        SysUser user = sysUserMapper.selectById(Long.valueOf(userId));
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "指定用户不存在: " + userId);
        }
        return userId;
    }

    private Long extractLongVar(DelegateExecution execution, String key) {
        Object val = execution.getVariable(key);
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return Long.valueOf(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
