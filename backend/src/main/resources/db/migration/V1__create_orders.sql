CREATE TABLE IF NOT EXISTS orders (
    id             BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no       VARCHAR(32)     NOT NULL COMMENT '订单号',
    customer_name  VARCHAR(50)     NOT NULL COMMENT '客户姓名',
    customer_phone VARCHAR(20)     NOT NULL COMMENT '客户电话',
    product_name   VARCHAR(100)    NOT NULL COMMENT '商品名称',
    quantity       INT             NOT NULL COMMENT '购买数量',
    unit_price     DECIMAL(10, 2)  NOT NULL COMMENT '商品单价',
    total_amount   DECIMAL(12, 2)  NOT NULL COMMENT '订单总金额',
    status         VARCHAR(20)     NOT NULL COMMENT '订单状态',
    remark         VARCHAR(255)    NULL COMMENT '备注',
    created_at     DATETIME(6)     NOT NULL COMMENT '创建时间',
    updated_at     DATETIME(6)     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_order_no (order_no),
    KEY idx_orders_status (status),
    KEY idx_orders_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '订单表';
