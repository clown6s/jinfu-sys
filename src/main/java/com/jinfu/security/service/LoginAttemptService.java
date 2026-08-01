package com.jinfu.security.service;

import com.jinfu.common.constant.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Login brute-force protection.
 * Max {@value #MAX_ATTEMPTS} failures within {@value #WINDOW_MINUTES} minutes per username,
 * then the account is locked for {@value #LOCK_MINUTES} minutes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MINUTES = 10;
    private static final long LOCK_MINUTES = 10;

    private final StringRedisTemplate redisTemplate;

    public boolean isLocked(String username) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey(username)));
    }

    public long getLockRemainingMinutes(String username) {
        Long ttl = redisTemplate.getExpire(lockKey(username), TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) {
            return LOCK_MINUTES;
        }
        return (ttl + 59) / 60;
    }

    public void onLoginFail(String username) {
        String failKey = failKey(username);
        Long fails = redisTemplate.opsForValue().increment(failKey);
        if (fails == null) {
            return;
        }
        if (fails == 1L) {
            redisTemplate.expire(failKey, WINDOW_MINUTES, TimeUnit.MINUTES);
        }
        if (fails >= MAX_ATTEMPTS) {
            redisTemplate.opsForValue().set(lockKey(username), "1", LOCK_MINUTES, TimeUnit.MINUTES);
            redisTemplate.delete(failKey);
            log.warn("Login locked for user [{}] after {} consecutive failures", username, fails);
        }
    }

    public void onLoginSuccess(String username) {
        redisTemplate.delete(failKey(username));
        redisTemplate.delete(lockKey(username));
    }

    private String failKey(String username) {
        return SecurityConstants.LOGIN_FAIL_PREFIX + username;
    }

    private String lockKey(String username) {
        return SecurityConstants.LOGIN_LOCK_PREFIX + username;
    }
}
