package com.ordercenter.order.log.dto;

import com.ordercenter.order.log.OrderLogAction;
import com.ordercenter.order.log.OrderOperationLog;

import java.time.LocalDateTime;

public record OrderLogResponse(
        Long id,
        String action,
        String actionLabel,
        String description,
        String operator,
        LocalDateTime createdAt
) {

    public static OrderLogResponse from(OrderOperationLog log) {
        String label;
        try {
            label = OrderLogAction.valueOf(log.getAction()).getLabel();
        } catch (IllegalArgumentException exception) {
            label = log.getAction();
        }
        return new OrderLogResponse(
                log.getId(),
                log.getAction(),
                label,
                log.getDescription(),
                log.getOperator(),
                log.getCreatedAt()
        );
    }
}
