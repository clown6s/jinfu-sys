package com.jinfu.approval.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinfu.approval.dto.ApprovalNodeVO;
import com.jinfu.approval.dto.CompleteApprovalRequest;
import com.jinfu.approval.dto.ProcessInstanceVO;
import com.jinfu.approval.dto.StartProcessRequest;
import com.jinfu.approval.entity.SysApprovalNode;
import com.jinfu.approval.entity.SysProcessInstance;
import com.jinfu.approval.entity.SysProcessTemplate;
import com.jinfu.approval.mapper.SysApprovalNodeMapper;
import com.jinfu.approval.mapper.SysProcessInstanceMapper;
import com.jinfu.approval.mapper.SysProcessTemplateMapper;
import com.jinfu.approval.service.ProcessInstanceService;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.ResultCode;
import com.jinfu.form.entity.FormDefinition;
import com.jinfu.form.mapper.FormDefinitionMapper;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessInstanceServiceImpl
        extends ServiceImpl<SysProcessInstanceMapper, SysProcessInstance>
        implements ProcessInstanceService {

    private final SysProcessTemplateMapper templateMapper;
    private final SysApprovalNodeMapper approvalNodeMapper;
    private final FormDefinitionMapper formDefinitionMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstanceVO startProcess(StartProcessRequest request, Long userId, String userName, Long deptId) {
        // 1. 加载模板
        SysProcessTemplate template = templateMapper.selectById(request.getTemplateId());
        if (template == null || template.getStatus() != 0) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "审批模板不存在或已停用");
        }

        // 2. 加载表单定义（获取 schema 做快照）
        FormDefinition formDef = formDefinitionMapper.selectById(template.getFormId());
        if (formDef == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "关联表单不存在");
        }

        // 3. 解析步骤链
        JSONArray stepChain = JSONUtil.parseArray(template.getStepChain());
        if (stepChain.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "审批模板未配置步骤");
        }

        // 4. 创建审批实例
        SysProcessInstance instance = new SysProcessInstance();
        instance.setTemplateId(template.getId());
        instance.setTemplateName(template.getTemplateName());
        instance.setFormId(formDef.getId());
        instance.setFormSchemaSnapshot(formDef.getSchemaJson());
        instance.setFormData(JSONUtil.toJsonStr(request.getFormData()));
        instance.setTitle(request.getTitle());
        instance.setInitiatorId(userId);
        instance.setInitiatorName(userName);
        instance.setDeptId(deptId);
        instance.setCurrentStep(1);
        instance.setTotalSteps(stepChain.size());
        instance.setStepChainSnapshot(template.getStepChain());
        instance.setStatus("pending");

        // 处理抄送人
        List<Map<String, Object>> ccUserInfo = Collections.emptyList();
        if (request.getCcUserIds() != null && !request.getCcUserIds().isEmpty()) {
            ccUserInfo = request.getCcUserIds().stream()
                    .map(uid -> {
                        SysUser u = sysUserMapper.selectById(uid);
                        if (u == null) return null;
                        return Map.<String, Object>of("id", u.getId(), "name", u.getNickname());
                    })
                    .filter(Objects::nonNull)
                    .toList();
            instance.setCcUsers(JSONUtil.toJsonStr(ccUserInfo));
        }

        save(instance);

        // 5. 为每个步骤创建审批节点（pending 状态）
        List<SysApprovalNode> nodes = new ArrayList<>();
        for (int i = 0; i < stepChain.size(); i++) {
            JSONObject step = stepChain.getJSONObject(i);
            int order = step.getInt("order");
            String stepName = step.getStr("name");
            String approverType = step.getStr("approverType");
            String approverValue = step.getStr("approverValue", "");

            SysApprovalNode node = new SysApprovalNode();
            node.setInstanceId(instance.getId());
            node.setStepOrder(order);
            node.setStepName(stepName);
            node.setApproverType(approverType);
            node.setApproverValue(approverValue);
            node.setAction("pending");

            // 解析审批人
            resolveApprover(node, instance, step);

            approvalNodeMapper.insert(node);
            nodes.add(node);
        }

        // 6. 推送通知给第一个审批人
        sendNotification(nodes.get(0));

        // 7. 推送抄送通知
        if (!ccUserInfo.isEmpty()) {
            for (Map<String, Object> cc : ccUserInfo) {
                Long ccId = ((Number) cc.get("id")).longValue();
                sendNotificationToUser(ccId,
                        String.format("【抄送】%s 发起了审批【%s】，请知悉", userName, request.getTitle()));
            }
        }

        // 8. 返回详情
        return buildVO(instance, nodes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeApproval(CompleteApprovalRequest request, Long userId, String userName) {
        // 1. 查找节点
        SysApprovalNode node = approvalNodeMapper.selectById(request.getNodeId());
        if (node == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "审批节点不存在");
        }
        if (!"pending".equals(node.getAction())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "该节点已审批过");
        }
        if (!userId.equals(node.getApproverId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "您不是该节点的审批人");
        }

        // 2. 查找实例
        SysProcessInstance instance = getById(node.getInstanceId());
        if (instance == null || !"pending".equals(instance.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "审批实例状态异常");
        }

        // 3. 更新节点
        node.setAction(request.getAction());
        node.setComment(request.getComment());
        node.setUpdateTime(LocalDateTime.now());
        approvalNodeMapper.updateById(node);

        // 4. 处理审批结果
        if ("rejected".equals(request.getAction())) {
            // 驳回→结束流程
            instance.setStatus("rejected");
            instance.setUpdateTime(LocalDateTime.now());
            updateById(instance);

            // 取消后续 pending 节点
            cancelPendingNodes(instance.getId(), node.getStepOrder());

            // 通知发起人
            sendNotificationToUser(instance.getInitiatorId(),
                    String.format("您的审批【%s】已被【%s】驳回", instance.getTitle(), node.getStepName()));
        } else {
            // 同意→检查是否还有下一步
            List<SysApprovalNode> allNodes = approvalNodeMapper.selectByInstanceId(instance.getId());
            int nextStep = node.getStepOrder() + 1;

            // 跳过条件不满足的节点
            SysApprovalNode nextNode = findNextValidNode(allNodes, nextStep, instance);

            if (nextNode == null) {
                // 全部通过
                instance.setStatus("approved");
                instance.setUpdateTime(LocalDateTime.now());
                updateById(instance);

                sendNotificationToUser(instance.getInitiatorId(),
                        String.format("您的审批【%s】已全部通过", instance.getTitle()));
            } else {
                // 推进到下一步
                instance.setCurrentStep(nextNode.getStepOrder());
                instance.setUpdateTime(LocalDateTime.now());
                updateById(instance);

                sendNotification(nextNode);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelProcess(Long instanceId, Long userId) {
        SysProcessInstance instance = getById(instanceId);
        if (instance == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "审批实例不存在");
        }
        if (!userId.equals(instance.getInitiatorId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能撤销自己发起的审批");
        }
        if (!"pending".equals(instance.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "只能撤销审批中的申请");
        }

        instance.setStatus("cancelled");
        updateById(instance);

        // 取消所有 pending 节点
        cancelPendingNodes(instanceId, 0);
    }

    @Override
    public ProcessInstanceVO getDetail(Long instanceId) {
        SysProcessInstance instance = getById(instanceId);
        if (instance == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "审批实例不存在");
        }
        List<SysApprovalNode> nodes = approvalNodeMapper.selectByInstanceId(instanceId);
        return buildVO(instance, nodes);
    }

    @Override
    public IPage<ProcessInstanceVO> myApplications(Page<SysProcessInstance> page, Long userId) {
        IPage<SysProcessInstance> resultPage = baseMapper.selectMyApplications(page, userId);
        return resultPage.convert(instance -> {
            List<SysApprovalNode> nodes = approvalNodeMapper.selectByInstanceId(instance.getId());
            return buildVO(instance, nodes);
        });
    }

    @Override
    public IPage<ProcessInstanceVO> todoApprovals(Page<SysProcessInstance> page, Long userId) {
        IPage<SysProcessInstance> resultPage = baseMapper.selectTodoApprovals(page, userId);
        return resultPage.convert(instance -> {
            List<SysApprovalNode> nodes = approvalNodeMapper.selectByInstanceId(instance.getId());
            return buildVO(instance, nodes);
        });
    }

    @Override
    public IPage<ProcessInstanceVO> doneApprovals(Page<SysProcessInstance> page, Long userId) {
        IPage<SysProcessInstance> resultPage = baseMapper.selectDoneApprovals(page, userId);
        return resultPage.convert(instance -> {
            List<SysApprovalNode> nodes = approvalNodeMapper.selectByInstanceId(instance.getId());
            return buildVO(instance, nodes);
        });
    }

    // ==================== 私有方法 ====================

    /**
     * 解析审批人（设置 approverId 和 approverName）
     */
    private void resolveApprover(SysApprovalNode node, SysProcessInstance instance, JSONObject step) {
        String type = node.getApproverType();
        String value = node.getApproverValue();

        switch (type) {
            case "specific_user" -> {
                SysUser user = sysUserMapper.selectById(Long.valueOf(value));
                if (user == null) throw new BusinessException(ResultCode.DATA_NOT_EXIST, "指定审批人不存在: " + value);
                node.setApproverId(user.getId());
                node.setApproverName(user.getNickname());
            }
            case "role" -> {
                List<SysUser> users = findUsersByRoleKey(value);
                if (users.isEmpty()) {
                    log.warn("角色 {} 下没有用户，跳过审批人设置", value);
                    node.setApproverId(0L);
                    node.setApproverName("角色-" + value);
                } else {
                    // 取第一个用户作为审批人（实际可扩展为多人会签）
                    SysUser user = users.get(0);
                    node.setApproverId(user.getId());
                    node.setApproverName(user.getNickname());
                }
            }
            case "dept_leader" -> {
                Long deptId = StrUtil.isNotBlank(value) ? Long.valueOf(value) : instance.getDeptId();
                SysDept dept = sysDeptMapper.selectById(deptId);
                if (dept == null || StrUtil.isBlank(dept.getLeader())) {
                    throw new BusinessException(ResultCode.DATA_NOT_EXIST, "部门未设置负责人: deptId=" + deptId);
                }
                // 按 leader 名称查找用户
                SysUser leader = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getDeptId, deptId)
                        .eq(SysUser::getNickname, dept.getLeader())
                        .last("LIMIT 1"));
                if (leader == null) {
                    throw new BusinessException(ResultCode.DATA_NOT_EXIST,
                            "未找到部门负责人用户: " + dept.getLeader());
                }
                node.setApproverId(leader.getId());
                node.setApproverName(leader.getNickname());
            }
            default -> throw new BusinessException(ResultCode.PARAM_INVALID, "未知的审批人类型: " + type);
        }
    }

    /**
     * 根据角色 key 查找用户列表
     */
    private List<SysUser> findUsersByRoleKey(String roleKey) {
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, roleKey)
                .eq(SysRole::getStatus, 0));
        if (role == null) return List.of();

        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, role.getId()));
        if (userRoles.isEmpty()) return List.of();

        List<Long> userIds = userRoles.stream().map(SysUserRole::getUserId).toList();
        return sysUserMapper.selectBatchIds(userIds);
    }

    /**
     * 查找下一个有效的审批节点（跳过条件不满足的节点）
     */
    private SysApprovalNode findNextValidNode(List<SysApprovalNode> allNodes, int startStep, SysProcessInstance instance) {
        if (startStep > instance.getTotalSteps()) return null;

        JSONObject formData = JSONUtil.parseObj(instance.getFormData());
        JSONArray stepChain = JSONUtil.parseArray(instance.getStepChainSnapshot());

        for (int i = startStep - 1; i < allNodes.size(); i++) {
            SysApprovalNode node = allNodes.get(i);
            // 检查条件
            JSONObject stepDef = stepChain.getJSONObject(i);
            if (stepDef != null && stepDef.containsKey("condition")) {
                String condition = stepDef.getStr("condition");
                if (StrUtil.isNotBlank(condition) && !evaluateCondition(condition, formData)) {
                    log.info("步骤 {} 条件不满足，跳过: {}", node.getStepName(), condition);
                    // 将该节点标记为 skipped 类型（可选）
                    node.setAction("skipped");
                    approvalNodeMapper.updateById(node);
                    continue;
                }
            }
            return node;
        }
        return null;
    }

    /**
     * 简单条件表达式求值（支持 > < >= <= ==）
     * 例: "amount > 5000", "leave_days > 3"
     */
    private boolean evaluateCondition(String condition, JSONObject data) {
        try {
            condition = condition.trim();
            String[] parts;
            String operator;
            if (condition.contains(">=")) {
                parts = condition.split(">=");
                operator = ">=";
            } else if (condition.contains("<=")) {
                parts = condition.split("<=");
                operator = "<=";
            } else if (condition.contains(">")) {
                parts = condition.split(">");
                operator = ">";
            } else if (condition.contains("<")) {
                parts = condition.split("<");
                operator = "<";
            } else if (condition.contains("==")) {
                parts = condition.split("==");
                operator = "==";
            } else {
                return true; // 无法解析视为满足
            }

            String field = parts[0].trim();
            double value = Double.parseDouble(parts[1].trim());
            double actual = data.getDouble(field, 0.0);

            return switch (operator) {
                case ">" -> actual > value;
                case "<" -> actual < value;
                case ">=" -> actual >= value;
                case "<=" -> actual <= value;
                case "==" -> actual == value;
                default -> true;
            };
        } catch (Exception e) {
            log.warn("条件表达式求值失败: {}", condition, e);
            return true; // 出错时默认满足，避免阻塞流程
        }
    }

    /**
     * 取消指定步骤之后的所有 pending 节点
     */
    private void cancelPendingNodes(Long instanceId, int afterStep) {
        List<SysApprovalNode> nodes = approvalNodeMapper.selectByInstanceId(instanceId);
        for (SysApprovalNode n : nodes) {
            if (n.getStepOrder() > afterStep && "pending".equals(n.getAction())) {
                n.setAction("cancelled");
                approvalNodeMapper.updateById(n);
            }
        }
    }

    /**
     * 发送待办通知给节点审批人
     */
    private void sendNotification(SysApprovalNode node) {
        if (node.getApproverId() != null && node.getApproverId() > 0) {
            SysProcessInstance instance = getById(node.getInstanceId());
            String msg = String.format("您有新的审批待办：【%s】—— %s", instance.getTitle(), node.getStepName());
            sendNotificationToUser(node.getApproverId(), msg);
        }
    }

    /**
     * 向指定用户推送消息
     */
    private void sendNotificationToUser(Long userId, String message) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "approval");
            payload.put("message", message);
            payload.put("timestamp", System.currentTimeMillis());
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId), "/queue/notifications", payload);
            log.info("WebSocket 推送成功: userId={}, msg={}", userId, message);
        } catch (Exception e) {
            log.error("WebSocket 推送失败: userId={}", userId, e);
        }
    }

    /**
     * 构建 VO
     */
    private ProcessInstanceVO buildVO(SysProcessInstance instance, List<SysApprovalNode> nodes) {
        ProcessInstanceVO vo = new ProcessInstanceVO();
        vo.setId(instance.getId());
        vo.setTemplateId(instance.getTemplateId());
        vo.setTemplateName(instance.getTemplateName());
        vo.setFormId(instance.getFormId());
        vo.setFormSchemaSnapshot(instance.getFormSchemaSnapshot());
        vo.setFormData(instance.getFormData());
        vo.setTitle(instance.getTitle());
        vo.setInitiatorId(instance.getInitiatorId());
        vo.setInitiatorName(instance.getInitiatorName());
        vo.setDeptId(instance.getDeptId());
        vo.setCurrentStep(instance.getCurrentStep());
        vo.setTotalSteps(instance.getTotalSteps());
        vo.setStatus(instance.getStatus());
        vo.setCreateTime(instance.getCreateTime());
        vo.setUpdateTime(instance.getUpdateTime());

        // 抄送人
        if (StrUtil.isNotBlank(instance.getCcUsers())) {
            List<?> rawList = JSONUtil.toList(instance.getCcUsers(), Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> typed = (List<Map<String, Object>>) rawList;
            vo.setCcUsers(typed);
        }

        if (nodes != null) {
            vo.setNodes(nodes.stream().map(n -> {
                ApprovalNodeVO nvo = new ApprovalNodeVO();
                nvo.setId(n.getId());
                nvo.setInstanceId(n.getInstanceId());
                nvo.setStepOrder(n.getStepOrder());
                nvo.setStepName(n.getStepName());
                nvo.setApproverType(n.getApproverType());
                nvo.setApproverValue(n.getApproverValue());
                nvo.setApproverId(n.getApproverId());
                nvo.setApproverName(n.getApproverName());
                nvo.setAction(n.getAction());
                nvo.setComment(n.getComment());
                nvo.setCreateTime(n.getCreateTime());
                nvo.setUpdateTime(n.getUpdateTime());
                return nvo;
            }).collect(Collectors.toList()));
        }

        return vo;
    }
}
