package com.example.hospital.common.cache;

public final class RedisKeyConstants {

    public static final String GLOBAL_PREFIX = "hospital:";
    public static final String AUTH_TOKEN_PREFIX = GLOBAL_PREFIX + "auth:token:";
    public static final String AUTH_USER_TOKENS_PREFIX = GLOBAL_PREFIX + "auth:user:";
    public static final String SCHEDULE_SLOTS_PREFIX = GLOBAL_PREFIX + "schedule:slots:";
    public static final String SCHEDULE_LOCK_PREFIX = GLOBAL_PREFIX + "lock:schedule:";

    private RedisKeyConstants() {
    }

    public static String authToken(String tokenHash) {
        return AUTH_TOKEN_PREFIX + tokenHash;
    }

    public static String authUserTokens(Long userId) {
        return AUTH_USER_TOKENS_PREFIX + userId;
    }

    public static String scheduleSlots(Long scheduleId) {
        return SCHEDULE_SLOTS_PREFIX + scheduleId;
    }

    public static String scheduleLock(Long scheduleId) {
        return SCHEDULE_LOCK_PREFIX + scheduleId;
    }
}
