package com.jinfu.config;

import cn.hutool.core.util.StrUtil;
import com.jinfu.security.entity.LoginUser;
import com.jinfu.security.service.TokenService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket 握手拦截器 — 从 URL 参数或 Header 中提取 JWT Token 验证用户身份
 * 客户端连接: ws://host/ws?token=xxx
 */
@Slf4j
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private TokenService tokenService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            // 1. 先从 URL 参数取 token
            String token = servletRequest.getServletRequest().getParameter("token");

            // 2. 再从 Header 取
            if (StrUtil.isBlank(token)) {
                token = servletRequest.getServletRequest().getHeader("Authorization");
                if (StrUtil.isNotBlank(token) && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }
            }

            if (StrUtil.isBlank(token)) {
                log.warn("WebSocket 握手失败: 缺少 token");
                return false;
            }

            try {
                LoginUser loginUser = tokenService.resolveToken(token);
                attributes.put("user", loginUser);
                // 设置 Principal 以便 SimpMessagingTemplate.convertAndSendToUser 使用
                servletRequest.getServletRequest().setAttribute("SPRING.PRINCIPAL", loginUser);
                log.debug("WebSocket 握手成功: userId={}", loginUser.getUserId());
                return true;
            } catch (Exception e) {
                log.warn("WebSocket 握手失败: token 无效", e);
                return false;
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
