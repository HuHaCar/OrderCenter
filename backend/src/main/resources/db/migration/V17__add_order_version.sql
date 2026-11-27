-- 分布式锁的应用层兜底：防止绕过锁或直接改库时并发覆盖
ALTER TABLE orders
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';
