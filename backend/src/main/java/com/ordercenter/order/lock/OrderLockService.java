package com.ordercenter.order.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 订单写操作的分布式锁。
 *
 * 锁必须在事务外获取：Controller 先调用这里，再在 action 中进入 @Transactional 方法。
 * 批量操作按订单 ID 排序加锁，避免两个请求以相反顺序加锁造成死锁。
 */
@Service
public class OrderLockService {

    private static final Logger log = LoggerFactory.getLogger(OrderLockService.class);
    private static final long WAIT_SECONDS = 3;
    private static final String KEY_PREFIX = "order:lock:{";
    private static final String KEY_SUFFIX = "}";

    private final RedissonClient redissonClient;

    public OrderLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public <T> T withOrderLock(Long orderId, Supplier<T> action) {
        return withOrderLocks(List.of(orderId), action);
    }

    public <T> T withOrderLocks(Iterable<Long> orderIds, Supplier<T> action) {
        List<Long> ids = new ArrayList<>();
        orderIds.forEach(ids::add);
        List<Long> sortedIds = ids.stream()
                .filter(id -> id != null)
                .distinct()
                .sorted()
                .toList();

        List<RLock> acquiredLocks = new ArrayList<>();

        try {
            // 按 ID 升序加锁，避免批量删除时互相死锁
            for (Long orderId : sortedIds) {
                RLock lock = acquire(orderId);
                acquiredLocks.add(lock);
            }

            // 锁外调用事务方法，事务在锁内执行
            return action.get();
        } finally {
            // 反向释放已经获取的锁
            List<RLock> reversed = new ArrayList<>(acquiredLocks);
            Collections.reverse(reversed);
            for (RLock lock : reversed) {
                try {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                } catch (RuntimeException exception) {
                    // 不因解锁失败覆盖已经完成的业务结果，锁最终会按 TTL 释放
                    log.warn("释放订单锁失败", exception);
                }
            }
        }
    }

    private RLock acquire(Long orderId) {
        // 只是创建一个 RLock 对象，并不会真正去加锁
        RLock lock = redissonClient.getLock(KEY_PREFIX + orderId + KEY_SUFFIX);
        try {
            // 尝试加锁
            // 如果锁当前空闲，立即获取成功，返回 true；
            // 如果锁被别的线程/实例持有，当前线程会最多等待 WAIT_SECONDS 秒，期间不断重试；
            // 等待超时仍拿不到锁，返回 false。
            if (!lock.tryLock(WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new OrderLockedException("订单正在被其他操作处理，请稍后重试");
            }
            return lock;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OrderLockedException("订单操作被中断，请稍后重试", exception);
        } catch (OrderLockedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            // Redisson 连接异常等：锁服务不可用时拒绝写操作，避免并发绕过
            throw new OrderLockedException("订单锁服务暂不可用，请稍后重试", exception);
        }
    }
}
