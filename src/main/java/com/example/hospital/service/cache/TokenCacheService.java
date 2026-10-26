package com.example.hospital.service.cache;

import com.example.hospital.common.cache.RedisKeyConstants;
import com.example.hospital.common.context.LoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;

@Service
public class TokenCacheService {

    private static final Logger log = LoggerFactory.getLogger(TokenCacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public TokenCacheService(RedisTemplate<String, Object> redisTemplate,
                             StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void cacheToken(String token, LoginUser loginUser, Duration ttl) {
        if (token == null || token.isBlank() || loginUser == null || loginUser.id() == null) {
            return;
        }
        try {
            String tokenHash = hashToken(token);
            redisTemplate.opsForValue().set(RedisKeyConstants.authToken(tokenHash), loginUser, ttl);
            String userTokensKey = RedisKeyConstants.authUserTokens(loginUser.id());
            stringRedisTemplate.opsForSet().add(userTokensKey, tokenHash);
            stringRedisTemplate.expire(userTokensKey, ttl);
        } catch (RuntimeException exception) {
            log.warn("Redis token cache is unavailable during write: {}", exception.getMessage());
        }
    }

    public TokenLookup lookup(String token) {
        if (token == null || token.isBlank()) {
            return new TokenLookup(TokenLookupState.MISSING, null);
        }
        try {
            Object cached = redisTemplate.opsForValue().get(RedisKeyConstants.authToken(hashToken(token)));
            if (cached == null) {
                return new TokenLookup(TokenLookupState.MISSING, null);
            }
            if (cached instanceof LoginUser loginUser) {
                return new TokenLookup(TokenLookupState.ACTIVE, loginUser);
            }
            log.warn("Unexpected cached token value type: {}", cached.getClass().getName());
            return new TokenLookup(TokenLookupState.MISSING, null);
        } catch (RuntimeException exception) {
            log.warn("Redis token cache is unavailable during read: {}", exception.getMessage());
            return new TokenLookup(TokenLookupState.UNAVAILABLE, null);
        }
    }

    public void removeToken(String token, Long userId) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            String tokenHash = hashToken(token);
            stringRedisTemplate.delete(RedisKeyConstants.authToken(tokenHash));
            if (userId != null) {
                stringRedisTemplate.opsForSet().remove(
                        RedisKeyConstants.authUserTokens(userId), tokenHash);
            }
        } catch (RuntimeException exception) {
            log.warn("Redis token cache is unavailable during removal: {}", exception.getMessage());
        }
    }

    public void invalidateUser(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            String userTokensKey = RedisKeyConstants.authUserTokens(userId);
            Set<String> tokenHashes = stringRedisTemplate.opsForSet().members(userTokensKey);
            if (tokenHashes != null && !tokenHashes.isEmpty()) {
                stringRedisTemplate.delete(tokenHashes.stream()
                        .map(RedisKeyConstants::authToken)
                        .toList());
            }
            stringRedisTemplate.delete(userTokensKey);
        } catch (RuntimeException exception) {
            log.warn("Redis token cache is unavailable during user invalidation: {}",
                    exception.getMessage());
        }
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public enum TokenLookupState {
        ACTIVE,
        MISSING,
        UNAVAILABLE
    }

    public record TokenLookup(TokenLookupState state, LoginUser user) {
    }
}
