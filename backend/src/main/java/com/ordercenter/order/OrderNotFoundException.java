package com.ordercenter.order;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long id) {
        super("订单不存在：" + id);
    }
}
