package com.ordercenter.order.lock;

/**
 * 订单正在被其他写操作处理，或分布式锁服务不可用。
 */
public class OrderLockedException extends RuntimeException {

    public OrderLockedException(String message) {
        super(message);
    }

    public OrderLockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
