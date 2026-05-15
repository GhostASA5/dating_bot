package com.project.ratingservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Периодически сбрасывает кэш ленты под распределённым lock в Redis (аналог фонового воркера).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeedCacheSweepJob {

    private static final String LOCK_KEY = "lock:feed-cache-sweep";

    private final RedisTemplate<String, String> redisTemplate;

    @Scheduled(fixedRate = 600_000)
    public void sweep() {
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "1", Duration.ofMinutes(4));
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }
        try {
            Set<String> keys = redisTemplate.keys("feed:*");
            if (keys == null || keys.isEmpty()) {
                return;
            }
            redisTemplate.delete(keys);
            log.info("rating.scheduler: cleared {} feed cache keys", keys.size());
        } catch (Exception e) {
            log.error("rating.scheduler: feed cache sweep failed: {}", e.getMessage(), e);
        } finally {
            redisTemplate.delete(LOCK_KEY);
        }
    }
}
