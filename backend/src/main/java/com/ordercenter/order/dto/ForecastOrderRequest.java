package com.ordercenter.order.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 预报订单请求实体。
 * 后续如果需要乐观锁或操作人信息，也可以继续加字段。
 */
public record ForecastOrderRequest(
        @NotNull(message = "订单 ID 不能为空")
        Long id
) {
}
