CREATE TABLE IF NOT EXISTS sales (
    id             BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    employee_no       VARCHAR(32)     NOT NULL COMMENT '工号',
    employee_name  VARCHAR(50)     NOT NULL COMMENT '业务员',
    lever         VARCHAR(20)     NOT NULL COMMENT '级别',
    entryDate     DATETIME(6)     NOT NULL COMMENT '入司时间',
    gender        TINYINT      DEFAULT NULL COMMENT '性别：0未知 1男 2女',
    phone         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    email         VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    id_card_no    VARCHAR(32)  DEFAULT NULL COMMENT '身份证号，敏感字段',
    remark         VARCHAR(255)    NULL COMMENT '备注',
    created_at     DATETIME(6)     NOT NULL COMMENT '创建时间',
    updated_at     DATETIME(6)     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sales_employee_no (employee_no),
    KEY idx_sales_lever (lever),
    KEY idx_sales_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '业务员表';
