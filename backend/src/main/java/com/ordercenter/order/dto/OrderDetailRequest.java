package com.ordercenter.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderDetailRequest(

        @NotNull(message = "订单ID不能为空")
        @Positive(message = "订单ID必须大于 0")
        Long id
) {
}
