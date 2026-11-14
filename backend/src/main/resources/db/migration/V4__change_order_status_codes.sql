-- 旧订单状态迁移为新单字母业务编码
-- PENDING   -> T 草稿
-- PAID      -> S 已预报
-- SHIPPED   -> D 已发货
-- COMPLETED -> V 已签收
-- CANCELLED -> C 已删除
-- 已存在的 T/S/R/D/F/V/P/C/Q 值保持不变
UPDATE orders
SET status = CASE status
    WHEN 'PENDING'   THEN 'T'
    WHEN 'PAID'      THEN 'S'
    WHEN 'SHIPPED'   THEN 'D'
    WHEN 'COMPLETED' THEN 'V'
    WHEN 'CANCELLED' THEN 'C'
    ELSE status
END;
