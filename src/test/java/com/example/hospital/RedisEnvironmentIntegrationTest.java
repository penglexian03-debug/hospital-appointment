package com.example.hospital;

import com.example.hospital.common.context.LoginUser;
import com.example.hospital.service.cache.RedisLockService;
import com.example.hospital.service.cache.ScheduleSlotCacheService;
import com.example.hospital.service.cache.TokenCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfSystemProperty(named = "redis.integration", matches = "true")
class RedisEnvironmentIntegrationTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private TokenCacheService tokenCacheService;

    @Autowired
    private ScheduleSlotCacheService scheduleSlotCacheService;

    @Autowired
    private RedisLockService redisLockService;

    @Test
    void stringRedisTemplateCanReachRedis() {
        String key = "hospital:test:environment:string";
        try {
            stringRedisTemplate.opsForValue().set(key, "ok", Duration.ofSeconds(10));
            assertThat(stringRedisTemplate.opsForValue().get(key)).isEqualTo("ok");

            RedisConnection connection = stringRedisTemplate.getConnectionFactory().getConnection();
            try {
                assertThat(connection.ping()).isEqualTo("PONG");
            } finally {
                connection.close();
            }
        } finally {
            stringRedisTemplate.delete(key);
        }
    }

    @Test
    void jsonValueSerializerRoundTripsLoginUser() {
        String key = "hospital:test:environment:json";
        LoginUser expected = new LoginUser(1L, "patient", "Test Patient", 1);
        try {
            redisTemplate.opsForValue().set(key, expected, Duration.ofSeconds(10));
            Object actual = redisTemplate.opsForValue().get(key);
            assertThat(actual).isInstanceOf(LoginUser.class);
            assertThat(actual).isEqualTo(expected);
        } finally {
            redisTemplate.delete(key);
        }
    }

    @Test
    void tokenCacheCanBeRemovedAndInvalidatedByUser() {
        LoginUser loginUser = new LoginUser(99L, "cache-user", "Cache User", 1);
        String firstToken = UUID.randomUUID().toString();
        String secondToken = UUID.randomUUID().toString();
        try {
            tokenCacheService.cacheToken(firstToken, loginUser, Duration.ofSeconds(30));
            tokenCacheService.cacheToken(secondToken, loginUser, Duration.ofSeconds(30));
            assertThat(tokenCacheService.lookup(firstToken).state())
                    .isEqualTo(TokenCacheService.TokenLookupState.ACTIVE);

            tokenCacheService.removeToken(firstToken, loginUser.id());
            assertThat(tokenCacheService.lookup(firstToken).state())
                    .isEqualTo(TokenCacheService.TokenLookupState.MISSING);
            assertThat(tokenCacheService.lookup(secondToken).state())
                    .isEqualTo(TokenCacheService.TokenLookupState.ACTIVE);

            tokenCacheService.invalidateUser(loginUser.id());
            assertThat(tokenCacheService.lookup(secondToken).state())
                    .isEqualTo(TokenCacheService.TokenLookupState.MISSING);
        } finally {
            tokenCacheService.removeToken(firstToken, loginUser.id());
            tokenCacheService.removeToken(secondToken, loginUser.id());
            tokenCacheService.invalidateUser(loginUser.id());
        }
    }

    @Test
    void scheduleSlotCacheLuaDeductsAndRestoresAtomically() {
        Long scheduleId = 987654321L;
        try {
            assertThat(scheduleSlotCacheService.initializeIfAbsent(scheduleId, 1)).isTrue();
            assertThat(scheduleSlotCacheService.tryDeduct(scheduleId))
                    .isEqualTo(ScheduleSlotCacheService.DeductResult.SUCCESS);
            assertThat(scheduleSlotCacheService.tryDeduct(scheduleId))
                    .isEqualTo(ScheduleSlotCacheService.DeductResult.SOLD_OUT);

            scheduleSlotCacheService.restore(scheduleId, 1);
            assertThat(scheduleSlotCacheService.findAvailableSlots(scheduleId).orElse(-1))
                    .isEqualTo(1);

            scheduleSlotCacheService.sync(scheduleId, 0);
            assertThat(scheduleSlotCacheService.tryDeduct(scheduleId))
                    .isEqualTo(ScheduleSlotCacheService.DeductResult.SOLD_OUT);
        } finally {
            scheduleSlotCacheService.evict(scheduleId);
        }
    }

    @Test
    void scheduleLockRejectsAnotherOwnerAndReleasesWithLuaTokenCheck() {
        Long scheduleId = 987654322L;
        Duration ttl = Duration.ofSeconds(10);
        try {
            RedisLockService.LockAttempt first = redisLockService.tryLock(scheduleId, ttl);
            assertThat(first.state()).isEqualTo(RedisLockService.LockState.ACQUIRED);
            assertThat(first.token()).isNotBlank();

            RedisLockService.LockAttempt second = redisLockService.tryLock(scheduleId, ttl);
            assertThat(second.state()).isEqualTo(RedisLockService.LockState.BUSY);

            assertThat(redisLockService.unlock(scheduleId, "wrong-token")).isFalse();
            assertThat(redisLockService.unlock(scheduleId, first.token())).isTrue();

            RedisLockService.LockAttempt third = redisLockService.tryLock(scheduleId, ttl);
            assertThat(third.state()).isEqualTo(RedisLockService.LockState.ACQUIRED);
            redisLockService.unlock(scheduleId, third.token());
        } finally {
            stringRedisTemplate.delete("hospital:lock:schedule:" + scheduleId);
        }
    }
}
