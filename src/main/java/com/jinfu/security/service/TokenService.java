package com.jinfu.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinfu.common.constant.SecurityConstants;
import com.jinfu.common.result.ResultCode;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.security.config.JwtProperties;
import com.jinfu.security.entity.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TokenService {

    @Resource
    private JwtProperties jwtProperties;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(LoginUser loginUser) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", loginUser.getUserId());
        claims.put("username", loginUser.getUsername());

        long now = System.currentTimeMillis();
        Date expiry = new Date(now + jwtProperties.getExpire() * 1000);

        String token = Jwts.builder()
                .claims(claims)
                .subject(loginUser.getUsername())
                .issuedAt(new Date(now))
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();

        // Cache LoginUser in Redis for quick lookup
        try {
            String userJson = objectMapper.writeValueAsString(loginUser);
            redisTemplate.opsForValue().set(
                    SecurityConstants.LOGIN_USER_KEY + loginUser.getUserId(),
                    userJson,
                    jwtProperties.getExpire(),
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.error("Failed to cache login user: {}", e.getMessage());
        }

        return token;
    }

    public LoginUser resolveToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        // Remove prefix
        if (token.startsWith(jwtProperties.getTokenPrefix())) {
            token = token.substring(jwtProperties.getTokenPrefix().length());
        }

        // Check blacklist
        if (isTokenBlacklisted(token)) {
            log.warn("Token is blacklisted");
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = claims.get("userId", Long.class);
            if (userId == null) {
                return null;
            }

            // Load from Redis
            String userJson = redisTemplate.opsForValue()
                    .get(SecurityConstants.LOGIN_USER_KEY + userId);
            if (userJson == null) {
                log.warn("LoginUser not found in Redis for userId: {}", userId);
                return null;
            }

            return objectMapper.readValue(userJson, LoginUser.class);
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to resolve token: {}", e.getMessage());
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
    }

    public void logout(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        if (token.startsWith(jwtProperties.getTokenPrefix())) {
            token = token.substring(jwtProperties.getTokenPrefix().length());
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = claims.get("userId", Long.class);
            if (userId != null) {
                redisTemplate.delete(SecurityConstants.LOGIN_USER_KEY + userId);
            }

            // Add token to blacklist with remaining TTL
            long remainingTtl = jwtProperties.getExpire();
            redisTemplate.opsForValue().set(
                    SecurityConstants.TOKEN_BLACKLIST_PREFIX + token,
                    "1",
                    remainingTtl,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.warn("Failed to logout: {}", e.getMessage());
        }
    }

    private boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(SecurityConstants.TOKEN_BLACKLIST_PREFIX + token));
    }
}
