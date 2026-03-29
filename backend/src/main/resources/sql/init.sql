-- 分库分表：两个库 demo_sharding_0 / demo_sharding_1，各含 t_order_0、t_order_1
-- create_time：TIMESTAMP(3)，JDBC serverTimezone=UTC，与 Java Instant 对齐

CREATE DATABASE IF NOT EXISTS demo_sharding_0 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS demo_sharding_1 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ---------- demo_sharding_0 ----------
USE demo_sharding_0;

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

-- ---------- demo_sharding_1 ----------
USE demo_sharding_1;

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
