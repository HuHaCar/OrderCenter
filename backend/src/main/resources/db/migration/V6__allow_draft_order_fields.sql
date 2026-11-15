-- 草稿订单只要求运输方式和收件国家，其余业务字段允许为空
ALTER TABLE orders
    MODIFY customer_name VARCHAR(50) NULL COMMENT '客户姓名',
    MODIFY customer_phone VARCHAR(20) NULL COMMENT '客户电话',
    MODIFY product_name VARCHAR(100) NULL COMMENT '商品名称',
    MODIFY quantity INT NULL COMMENT '购买数量',
    MODIFY unit_price DECIMAL(10, 2) NULL COMMENT '商品单价',
    MODIFY total_amount DECIMAL(12, 2) NULL COMMENT '订单总金额';
