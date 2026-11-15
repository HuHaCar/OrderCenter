package com.ordercenter.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量删除请求。单行删除也复用该接口，ids 长度为 1 即可。
 */
public record DeleteOrdersRequest(

        @NotEmpty(message = "请选择要删除的订单")
        @Size(max = 200, message = "单次最多删除 200 个订单")
        List<@NotNull(message = "订单 ID 不能为空") Long> ids
) {
}
