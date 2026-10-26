package com.example.hospital.service.cache;

import com.example.hospital.common.cache.RedisKeyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.OptionalInt;

@Service
public class ScheduleSlotCacheService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleSlotCacheService.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private static final DefaultRedisScript<Long> DEDUCT_SCRIPT = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('GET', KEYS[1]))
            if not current then
                return -2
            end
            if current <= 0 then
                return 0
            end
            redis.call('DECR', KEYS[1])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> RESTORE_SCRIPT = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('GET', KEYS[1]))
            if not current then
                return -2
            end
            local maximum = tonumber(ARGV[1])
            if current < maximum then
                return redis.call('INCR', KEYS[1])
            end
            return -1
            """, Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    public ScheduleSlotCacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public OptionalInt findAvailableSlots(Long scheduleId) {
        try {
            String value = stringRedisTemplate.opsForValue()
                    .get(RedisKeyConstants.scheduleSlots(scheduleId));
            return value == null ? OptionalInt.empty() : OptionalInt.of(Integer.parseInt(value));
        } catch (RuntimeException exception) {
            log.warn("Redis schedule cache is unavailable during read: {}", exception.getMessage());
            return OptionalInt.empty();
        }
    }

    public boolean initializeIfAbsent(Long scheduleId, int availableSlots) {
        try {
            Boolean initialized = stringRedisTemplate.opsForValue().setIfAbsent(
                    RedisKeyConstants.scheduleSlots(scheduleId),
                    String.valueOf(Math.max(availableSlots, 0)),
                    CACHE_TTL);
            return Boolean.TRUE.equals(initialized);
        } catch (RuntimeException exception) {
            log.warn("Redis schedule cache is unavailable during initialization: {}",
                    exception.getMessage());
            return false;
        }
    }

    public DeductResult tryDeduct(Long scheduleId) {
        try {
            Long result = stringRedisTemplate.execute(
                    DEDUCT_SCRIPT,
                    List.of(RedisKeyConstants.scheduleSlots(scheduleId)));
            if (result == null) {
                return DeductResult.UNAVAILABLE;
            }
            return switch (result.intValue()) {
                case 1 -> DeductResult.SUCCESS;
                case 0 -> DeductResult.SOLD_OUT;
                case -2 -> DeductResult.MISSING;
                default -> DeductResult.UNAVAILABLE;
            };
        } catch (RuntimeException exception) {
            log.warn("Redis schedule cache is unavailable during deduction: {}",
                    exception.getMessage());
            return DeductResult.UNAVAILABLE;
        }
    }

    public void restore(Long scheduleId, int maximumSlots) {
        try {
            stringRedisTemplate.execute(
                    RESTORE_SCRIPT,
                    List.of(RedisKeyConstants.scheduleSlots(scheduleId)),
                    String.valueOf(Math.max(maximumSlots, 0)));
        } catch (RuntimeException exception) {
            log.warn("Redis schedule cache is unavailable during restoration: {}",
                    exception.getMessage());
        }
    }

    public void sync(Long scheduleId, int availableSlots) {
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisKeyConstants.scheduleSlots(scheduleId),
                    String.valueOf(Math.max(availableSlots, 0)),
                    CACHE_TTL);
        } catch (RuntimeException exception) {
            log.warn("Redis schedule cache is unavailable during synchronization: {}",
                    exception.getMessage());
        }
    }

    public void evict(Long scheduleId) {
        try {
            stringRedisTemplate.delete(RedisKeyConstants.scheduleSlots(scheduleId));
        } catch (RuntimeException exception) {
            log.warn("Redis schedule cache is unavailable during eviction: {}",
                    exception.getMessage());
        }
    }

    public enum DeductResult {
        SUCCESS,
        SOLD_OUT,
        MISSING,
        UNAVAILABLE
    }
}
