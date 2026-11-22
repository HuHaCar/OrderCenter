ALTER TABLE sales
    ADD COLUMN current_month_performance DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '本月业绩' AFTER lever;
