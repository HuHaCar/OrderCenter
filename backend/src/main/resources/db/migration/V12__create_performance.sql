CREATE TABLE IF NOT EXISTS sales_performance (
    performance_id    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    sales_id          BIGINT        NOT NULL COMMENT '业务员ID，关联 sales.id',
    performance_month CHAR(7)       NOT NULL COMMENT '业绩月份，格式 yyyy-MM',
    amount            DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '业绩金额',
    created_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (performance_id),
    UNIQUE KEY uk_sales_perf_month (sales_id, performance_month),
    KEY idx_sales_perf_month (performance_month)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '业务员月度业绩表';