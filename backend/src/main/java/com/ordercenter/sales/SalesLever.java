package com.ordercenter.sales;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单状态。
 *
 * code 是数据库和接口使用的单字母业务编码；
 * label 是页面展示的中文名称。
 */
public enum SalesLever {

    DRAFT("A", "草稿"),
    FORECAST("B", "已预报"),
    RECEIVED("C", "已收货"),
    SHIPPED("D", "已发货");

    private final String code;
    private final String label;

    private static final Map<String, SalesLever> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(SalesLever::getCode, Function.identity()));

    SalesLever(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 按单字母 code 解析枚举。
     * code 为空时返回 null；无法识别时抛出 IllegalArgumentException。
     */
    public static SalesLever fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        SalesLever status = BY_CODE.get(code.trim().toUpperCase(Locale.ROOT));
        if (status == null) {
            throw new IllegalArgumentException("无效的订单状态：" + code.trim());
        }
        return status;
    }

}
