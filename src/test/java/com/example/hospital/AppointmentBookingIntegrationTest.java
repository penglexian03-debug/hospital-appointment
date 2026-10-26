package com.example.hospital;

import com.example.hospital.common.context.LoginUser;
import com.example.hospital.common.context.UserContext;
import com.example.hospital.common.exception.BusinessException;
import com.example.hospital.dto.AppointmentDTO;
import com.example.hospital.dto.CancelAppointmentDTO;
import com.example.hospital.service.AppointmentService;
import com.example.hospital.service.cache.ScheduleSlotCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfSystemProperty(named = "appointment.integration", matches = "true")
class AppointmentBookingIntegrationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ScheduleSlotCacheService scheduleSlotCacheService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void concurrentBookingsRespectCapacityAndCancellationRestoresCache() throws Exception {
        int slotCount = 3;
        int userCount = 8;
        boolean redisAvailable = redisAvailable();
        Long scheduleId = createSchedule(slotCount);
        List<Long> userIds = createUsers(userCount);
        ExecutorService executor = Executors.newFixedThreadPool(userCount);

        try {
            CountDownLatch ready = new CountDownLatch(userCount);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < userIds.size(); i++) {
                Long userId = userIds.get(i);
                String phone = String.format("138%08d", i + 1);
                futures.add(executor.submit(() -> {
                    UserContext.set(new LoginUser(userId, "it-user-" + userId,
                            "IT User " + userId, 1));
                    ready.countDown();
                    try {
                        start.await();
                        appointmentService.create(appointmentDTO(scheduleId, phone));
                        return true;
                    } catch (BusinessException exception) {
                        return false;
                    } finally {
                        UserContext.clear();
                    }
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int success = 0;
            int conflict = 0;
            for (Future<Boolean> future : futures) {
                if (future.get(20, TimeUnit.SECONDS)) {
                    success++;
                } else {
                    conflict++;
                }
            }

            assertThat(success).isEqualTo(slotCount);
            assertThat(conflict).isEqualTo(userCount - slotCount);
            assertThat(availableSlots(scheduleId)).isZero();
            assertThat(pendingAppointmentCount(scheduleId)).isEqualTo(slotCount);
            if (redisAvailable) {
                assertThat(scheduleSlotCacheService.findAvailableSlots(scheduleId).orElse(-1))
                        .isZero();
            }

            Map<String, Object> appointment = jdbcTemplate.queryForMap(
                    "SELECT id, user_id FROM appointment " +
                            "WHERE schedule_id = ? AND status = 0 ORDER BY id LIMIT 1",
                    scheduleId);
            long appointmentId = ((Number) appointment.get("id")).longValue();
            long ownerId = ((Number) appointment.get("user_id")).longValue();
            UserContext.set(new LoginUser(ownerId, "it-owner", "IT Owner", 1));
            assertThatThrownBy(() -> appointmentService.create(
                    appointmentDTO(scheduleId, "13800000001")))
                    .isInstanceOf(BusinessException.class);
            assertThat(availableSlots(scheduleId)).isZero();

            appointmentService.cancel(appointmentId, new CancelAppointmentDTO());
            assertThat(availableSlots(scheduleId)).isEqualTo(1);
            if (redisAvailable) {
                assertThat(scheduleSlotCacheService.findAvailableSlots(scheduleId).orElse(-1))
                        .isEqualTo(1);
            }
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT status FROM appointment WHERE id = ?", Integer.class, appointmentId))
                    .isEqualTo(2);
        } finally {
            UserContext.clear();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            cleanup(scheduleId, userIds);
        }
    }

    private AppointmentDTO appointmentDTO(Long scheduleId, String phone) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setScheduleId(scheduleId);
        dto.setPatientName("Integration Patient");
        dto.setPatientPhone(phone);
        return dto;
    }

    private Long createSchedule(int totalSlots) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDate workDate = LocalDate.now().plusDays(
                500L + ThreadLocalRandom.current().nextInt(1000));
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO schedule " +
                            "(doctor_id, work_date, period, start_time, end_time, " +
                            "total_slots, available_slots, registration_fee, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, 1L);
            statement.setDate(2, java.sql.Date.valueOf(workDate));
            statement.setInt(3, 1);
            statement.setTime(4, Time.valueOf(LocalTime.of(8, 0)));
            statement.setTime(5, Time.valueOf(LocalTime.of(12, 0)));
            statement.setInt(6, totalSlots);
            statement.setInt(7, totalSlots);
            statement.setBigDecimal(8, new java.math.BigDecimal("30.00"));
            statement.setInt(9, 1);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create integration schedule");
        }
        return key.longValue();
    }

    private List<Long> createUsers(int count) {
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int index = i;
            KeyHolder keyHolder = new GeneratedKeyHolder();
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            String phone = String.format("139%08d",
                    Math.floorMod(System.nanoTime() / 1000 + i * 7919L, 100_000_000L));
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO sys_user " +
                                "(username, password, real_name, phone, gender, role, status) " +
                                "VALUES (?, ?, ?, ?, 0, 1, 1)",
                        Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, "it_" + suffix);
                statement.setString(2, "integration-test-password");
                statement.setString(3, "IT User " + index);
                statement.setString(4, phone);
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("Failed to create integration user");
            }
            userIds.add(key.longValue());
        }
        return userIds;
    }

    private int availableSlots(Long scheduleId) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT available_slots FROM schedule WHERE id = ?", Integer.class, scheduleId);
        return value == null ? -1 : value;
    }

    private int pendingAppointmentCount(Long scheduleId) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appointment WHERE schedule_id = ? AND status = 0",
                Integer.class, scheduleId);
        return value == null ? 0 : value;
    }

    private boolean redisAvailable() {
        try {
            stringRedisTemplate.opsForValue().get("hospital:test:availability");
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void cleanup(Long scheduleId, List<Long> userIds) {
        if (scheduleId != null) {
            jdbcTemplate.update("DELETE FROM appointment WHERE schedule_id = ?", scheduleId);
            jdbcTemplate.update("DELETE FROM schedule WHERE id = ?", scheduleId);
            scheduleSlotCacheService.evict(scheduleId);
            try {
                stringRedisTemplate.delete("hospital:lock:schedule:" + scheduleId);
            } catch (RuntimeException ignored) {
                // Redis may be intentionally stopped during the fallback test.
            }
        }
        for (Long userId : userIds) {
            jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", userId);
        }
    }
}

