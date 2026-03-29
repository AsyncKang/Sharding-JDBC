-- 手动在 MySQL 执行（需已存在库 demo_sharding）
-- create_time 使用 TIMESTAMP：MySQL 按 UTC 存储，与 Java Instant 对齐；JDBC URL 使用 serverTimezone=UTC

USE demo_sharding;

DROP TABLE IF EXISTS t_order_0;
DROP TABLE IF EXISTS t_order_1;

CREATE TABLE t_order_0 (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    create_time TIMESTAMP(3) NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_user_create (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_order_1 (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    create_time TIMESTAMP(3) NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_user_create (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
