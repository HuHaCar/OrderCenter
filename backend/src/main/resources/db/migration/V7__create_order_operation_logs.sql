CREATE TABLE order_operation_logs (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id    BIGINT       NOT NULL COMMENT '订单 ID',
    order_no    VARCHAR(32)  NOT NULL COMMENT '订单号快照',
    action      VARCHAR(20)  NOT NULL COMMENT '操作类型',
    description VARCHAR(255) NOT NULL COMMENT '操作描述',
    operator    VARCHAR(50)  NOT NULL COMMENT '操作人',
    created_at  DATETIME(6)  NOT NULL COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_order_logs_order_created (order_id, created_at),
    CONSTRAINT fk_order_logs_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '订单操作日志';
