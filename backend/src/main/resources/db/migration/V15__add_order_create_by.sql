-- 订单创建者使用后台用户 ID（users.id），不是业务员 sales.id。
-- 历史订单没有创建者信息，因此允许为 NULL。
ALTER TABLE orders
    ADD COLUMN create_by BIGINT NULL COMMENT '创建者用户ID，关联 users.id' AFTER remark,
    ADD KEY idx_orders_create_by (create_by),
    ADD CONSTRAINT fk_orders_create_by
        FOREIGN KEY (create_by) REFERENCES users (id);
