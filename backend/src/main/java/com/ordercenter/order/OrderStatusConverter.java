package com.ordercenter.order;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * 让数据库存单字母 code，而不是 Java 枚举名。
 * 例如 Java 中是 SHIPPED，数据库里存 D。
 */
@Converter
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {

    @Override
    public String convertToDatabaseColumn(OrderStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public OrderStatus convertToEntityAttribute(String dbData) {
        return OrderStatus.fromCode(dbData);
    }
}
