package com.ordercenter.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 保存草稿时只强制运输方式和收件国家。
 * 其他字段允许不填；前端如果已经填了，也会一起保存。
 */
public record CreateDraftOrderRequest(

        @NotBlank(message = "请选择运输方式")
        @Size(max = 20, message = "运输方式代码不能超过 20 个字符")
        String shippingMethod,

        @NotBlank(message = "请选择收件国家")
        @Pattern(regexp = "^[A-Z]{2}$", message = "收件国家必须是二字 ISO 国家码")
        String recipientCountry,

        String customerName,
        String customerPhone,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        String remark
) {
}
