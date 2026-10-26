package com.example.hospital.service.cache;

import com.example.hospital.common.cache.RedisKeyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class RedisLockService {

    private static final Logger log = LoggerFactory.getLogger(RedisLockService.class);

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisLockService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public LockAttempt tryLock(Long scheduleId, Duration ttl) {
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(
                    RedisKeyConstants.scheduleLock(scheduleId), token, ttl);
            return Boolean.TRUE.equals(acquired)
                    ? new LockAttempt(LockState.ACQUIRED, token)
                    : new LockAttempt(LockState.BUSY, null);
        } catch (RuntimeException exception) {
            log.warn("Redis schedule lock is unavailable: {}", exception.getMessage());
            return new LockAttempt(LockState.UNAVAILABLE, null);
        }
    }

    public boolean unlock(Long scheduleId, String token) {
        if (token == null) {
            return false;
        }
        try {
            Long result = stringRedisTemplate.execute(
                    UNLOCK_SCRIPT,
                    List.of(RedisKeyConstants.scheduleLock(scheduleId)),
                    token);
            return result != null && result > 0;
        } catch (RuntimeException exception) {
            log.warn("Redis schedule lock is unavailable during release: {}",
                    exception.getMessage());
            return false;
        }
    }

    public enum LockState {
        ACQUIRED,
        BUSY,
        UNAVAILABLE
    }

    public record LockAttempt(LockState state, String token) {
    }
}
