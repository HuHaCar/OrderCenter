-- 列表按最近操作时间排序：更新时间倒序
ALTER TABLE orders
    ADD INDEX idx_orders_updated_at (updated_at);
