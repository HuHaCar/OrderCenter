package com.ordercenter.order.detail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordercenter.order.dto.OrderDetailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单详情 Cache-Aside 缓存。
 * Redis 只是加速层，Redis 异常时直接降级查数据库。
 */
@Service
public class OrderDetailCacheService {

    private static final Logger log = LoggerFactory.getLogger(OrderDetailCacheService.class);
    private static final String KEY_PREFIX = "order:detail:";
    private static final Duration BASE_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public OrderDetailCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public OrderDetailResponse get(Long orderId) {
        try {
            String value = redisTemplate.opsForValue().get(key(orderId));
            if (value == null) {
                return null;
            }
            return objectMapper.readValue(value, OrderDetailResponse.class);
        } catch (Exception exception) {
            log.warn("读取订单详情缓存失败，降级查询数据库：orderId={}", orderId, exception);
            return null;
        }
    }

    public void put(OrderDetailResponse detail) {
        try {
            Duration ttl = BASE_TTL.plusSeconds(
                    ThreadLocalRandom.current().nextInt(60)
            );
            redisTemplate.opsForValue().set(
                    key(detail.id()),
                    objectMapper.writeValueAsString(detail),
                    ttl
            );
        } catch (Exception exception) {
            log.warn("写入订单详情缓存失败：orderId={}", detail.id(), exception);
        }
    }

    public void evictAfterCommit(Long orderId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            evict(orderId);
                        }
                    }
            );
            return;
        }
        evict(orderId);
    }

    private void evict(Long orderId) {
        try {
            redisTemplate.delete(key(orderId));
        } catch (Exception exception) {
            log.warn("删除订单详情缓存失败：orderId={}", orderId, exception);
        }
    }

    private static String key(Long orderId) {
        return KEY_PREFIX + orderId;
    }
}
