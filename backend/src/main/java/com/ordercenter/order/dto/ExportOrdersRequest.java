package com.ordercenter.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量导出请求。前端选中的订单 ID 按列表顺序传入。
 */
public record ExportOrdersRequest(

        @NotEmpty(message = "请选择要导出的订单")
        @Size(max = 200, message = "单次最多导出 200 个订单")
        List<@NotNull(message = "订单 ID 不能为空") Long> ids
) {
}
