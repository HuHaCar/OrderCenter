package com.ordercenter.order;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单状态。
 *
 * code 是数据库和接口使用的单字母业务编码；
 * label 是页面展示的中文名称。
 */
public enum OrderStatus {

    DRAFT("T", "草稿"),
    FORECAST("S", "已预报"),
    RECEIVED("R", "已收货"),
    SHIPPED("D", "已发货"),
    RETURNED("F", "已退件"),
    SIGNED("V", "已签收"),
    CLAIMED("P", "已理赔"),
    DELETED("C", "已删除"),
    ABANDONED("Q", "弃件");

    private final String code;
    private final String label;

    private static final Map<String, OrderStatus> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(OrderStatus::getCode, Function.identity()));

    OrderStatus(String code, String label) {
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
    public static OrderStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        OrderStatus status = BY_CODE.get(code.trim().toUpperCase(Locale.ROOT));
        if (status == null) {
            throw new IllegalArgumentException("无效的订单状态：" + code.trim());
        }
        return status;
    }

    /**
     * 当前状态允许流转到的下一批状态。
     *
     * C（已删除）由列表删除动作直接设置，不属于普通流转目标。
     * 具体状态修改接口接入后，应在 Service 中调用 canTransitionTo 做校验。
     */
    public Set<OrderStatus> nextStatuses() {
        Set<OrderStatus> next = switch (this) {
            case DRAFT -> EnumSet.of(FORECAST);
            case FORECAST -> EnumSet.of(RECEIVED);
            case RECEIVED -> EnumSet.of(SHIPPED);
            case SHIPPED -> EnumSet.of(SIGNED, ABANDONED, CLAIMED);
            case SIGNED -> EnumSet.of(RETURNED);
            default -> EnumSet.noneOf(OrderStatus.class);
        };
        return Collections.unmodifiableSet(next);
    }

    public boolean canTransitionTo(OrderStatus next) {
        return next != null && nextStatuses().contains(next);
    }

    /** 只有草稿可以执行预报操作。 */
    public boolean isForecastable() {
        return this == DRAFT;
    }

    /** 只有草稿和已预报订单允许编辑。 */
    public boolean isEditable() {
        return this == DRAFT || this == FORECAST;
    }

    /** 删除功能暂未开放，规则先约定：草稿和已预报可删除。 */
    public boolean isDeletable() {
        return this == DRAFT || this == FORECAST;
    }
}
