package com.ordercenter.order.log.dto;

import jakarta.validation.constraints.NotNull;

public record OrderLogQueryRequest(
        @NotNull(message = "订单 ID 不能为空")
        Long orderId
) {
}
