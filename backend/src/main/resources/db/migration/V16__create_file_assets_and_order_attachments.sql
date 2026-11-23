CREATE TABLE IF NOT EXISTS file_assets (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    storage_key   VARCHAR(255) NOT NULL COMMENT '存储相对路径',
    content_type  VARCHAR(100) NOT NULL COMMENT 'MIME类型',
    file_size     BIGINT       NOT NULL COMMENT '文件大小，字节',
    sha256        CHAR(64)     NULL COMMENT '文件SHA-256',
    uploaded_by   BIGINT       NOT NULL COMMENT '上传用户ID',
    created_at    DATETIME(6)  NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_storage_key (storage_key),
    KEY idx_file_uploaded_by (uploaded_by),
    KEY idx_file_created_at (created_at),
    CONSTRAINT fk_file_uploader
        FOREIGN KEY (uploaded_by) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '文件资源表';

CREATE TABLE IF NOT EXISTS order_attachments (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id   BIGINT       NOT NULL COMMENT '订单ID',
    file_id    BIGINT       NOT NULL COMMENT '文件ID',
    created_at DATETIME(6)  NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_attachment_file (order_id, file_id),
    KEY idx_order_attachments_file (file_id),
    CONSTRAINT fk_order_attachment_order
        FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_attachment_file
        FOREIGN KEY (file_id) REFERENCES file_assets (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '订单附件关联表';
