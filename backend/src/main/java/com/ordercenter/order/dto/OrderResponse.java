package com.ordercenter.order.dto;

import com.ordercenter.order.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对外响应对象，避免直接暴露 JPA Entity。
 * status 返回单字母业务编码，例如 D；statusLabel 返回中文名称。
 */
public record OrderResponse(
        Long id,
        String orderNo,
        String customerName,
        String customerPhone,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String status,
        String statusLabel,
        String remark,
        LocalDateTime createdAt
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getProductName(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.getStatus().getCode(),
                order.getStatus().getLabel(),
                order.getRemark(),
                order.getCreatedAt()
        );
    }
}
