package com.ordercenter.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 新增订单请求。
 * 订单号、总金额、初始状态、创建时间均由后端生成，前端不传。
 */
public record CreateOrderRequest(

        @NotBlank(message = "请选择运输方式")
        @Size(max = 20, message = "运输方式代码不能超过 20 个字符")
        String shippingMethod,

        @NotBlank(message = "请选择收件国家")
        @Pattern(regexp = "^[A-Z]{2}$", message = "收件国家必须是二字 ISO 国家码")
        String recipientCountry,

        @NotBlank(message = "客户姓名不能为空")
        @Size(max = 50, message = "客户姓名不能超过 50 个字符")
        String customerName,

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String customerPhone,

        @NotBlank(message = "商品名称不能为空")
        @Size(max = 100, message = "商品名称不能超过 100 个字符")
        String productName,

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量必须大于 0")
        @Max(value = 9999, message = "数量不能超过 9999")
        Integer quantity,

        @NotNull(message = "单价不能为空")
        @DecimalMin(value = "0.01", message = "单价必须大于 0")
        @Digits(integer = 8, fraction = 2, message = "单价最多保留两位小数")
        BigDecimal unitPrice,

        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark
) {
}
