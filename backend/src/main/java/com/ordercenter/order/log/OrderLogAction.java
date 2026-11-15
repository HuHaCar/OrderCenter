package com.ordercenter.order.log;

public enum OrderLogAction {

    CREATE("创建"),
    DRAFT("保存草稿"),
    FORECAST("预报"),
    UPDATE("编辑"),
    DELETE("删除");

    private final String label;

    OrderLogAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
