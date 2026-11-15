-- 订单增加运输方式和收件国家
-- 老数据默认：THZXR（云途全球专线挂号（特惠带电），支持所有国家） + CN
ALTER TABLE orders
    ADD COLUMN shipping_method VARCHAR(20) NOT NULL DEFAULT 'THZXR' COMMENT '运输方式代码' AFTER order_no,
    ADD COLUMN recipient_country CHAR(2) NOT NULL DEFAULT 'CN' COMMENT '收件国家二字码' AFTER shipping_method;
