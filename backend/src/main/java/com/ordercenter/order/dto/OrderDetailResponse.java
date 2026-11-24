package com.ordercenter.order.dto;

import com.ordercenter.file.dto.FileAssetResponse;
import com.ordercenter.order.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情响应。附件只返回文件元数据，不返回磁盘路径。
 */
public record OrderDetailResponse(
        Long id,
        String orderNo,
        String shippingMethod,
        String shippingMethodLabel,
        String recipientCountry,
        String customerName,
        String customerPhone,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String status,
        String statusLabel,
        String remark,
        Long createBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<FileAssetResponse> attachments
) {

    public static OrderDetailResponse from(
            Order order,
            String shippingMethodLabel,
            List<FileAssetResponse> attachments
    ) {
        return new OrderDetailResponse(
                order.getId(),
                order.getOrderNo(),
                order.getShippingMethod(),
                shippingMethodLabel,
                order.getRecipientCountry(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getProductName(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.getStatus().getCode(),
                order.getStatus().getLabel(),
                order.getRemark(),
                order.getCreateBy(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                attachments == null ? List.of() : List.copyOf(attachments)
        );
    }
}
