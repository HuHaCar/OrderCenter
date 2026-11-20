-- V10 使用的列名是 entryDate；Hibernate 的命名策略会映射为 entry_date。
-- 为了保持数据库列名规范和 Entity 映射一致，这里重命名列。
ALTER TABLE sales
    RENAME COLUMN entryDate TO entry_date;
