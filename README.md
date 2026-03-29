# ShardingSphere-JDBC 学习 Demo（Spring Boot + MyBatis + Vue）

**简介**：Apache ShardingSphere-JDBC 入门示例——单数据源、逻辑表 `t_order` 按 `user_id` 范围分片到两张物理表，集成 MyBatis、PageHelper、Vue 3；适合对照官方文档理解路由改写与分片键设计。

> **名称说明**：常说的「Sharding-JDBC」已并入 **[Apache ShardingSphere](https://shardingsphere.apache.org/)**，以 **`shardingsphere-jdbc`** 形式提供，通过 **`ShardingSphereDriver`** + **`classpath:sharding.yaml`** 接入 Spring Boot（[官方：Spring Boot 与 JDBC 驱动](https://shardingsphere.apache.org/document/current/en/user-manual/shardingsphere-jdbc/yaml-config/jdbc-driver/spring-boot/)）。

本仓库是一个**最小可运行示例**：单库两张物理表、逻辑表 `t_order`、范围分片、分页与 Vue 联调，便于你按下面「学习路径」循序渐进，再深入官方文档。

---

## 目录结构

```
.
├── backend/                    # Spring Boot + MyBatis + ShardingSphere-JDBC
│   └── src/main/resources/
│       ├── application.yml     # 数据源指向 ShardingSphere 驱动
│       ├── sharding.yaml       # 分片规则（核心）
│       ├── mapper/             # MyBatis：只写逻辑表名
│       └── sql/init.sql        # 手工建表脚本（无 Flyway）
└── frontend/                   # Vue 3 + Vite
```

---

## 本示例在演示什么

| 要点 | 说明 |
|------|------|
| 逻辑表 vs 物理表 | SQL/MyBatis 中只出现 **`t_order`**，运行时改写为 **`t_order_0` / `t_order_1`**。 |
| 分片键 | **`user_id`**，带该条件时可路由到单分片。 |
| 分片算法 | **`autoTables` + `BOUNDARY_RANGE`**：`user_id < 10^9` → `t_order_0`，否则 → `t_order_1`（边界见 `OrderShardingConstants` 与 `sharding.yaml`）。 |
| 主键 | 逻辑列 **`id`** 由 ShardingSphere **SNOWFLAKE** 生成。 |
| SQL 观察 | `sharding.yaml` 中 **`sql-show: true`**，控制台可看**改写后的 SQL**。 |

---

## 环境准备与启动

### 1. 数据库

1. 安装 **MySQL**，创建库：

```sql
CREATE DATABASE IF NOT EXISTS demo_sharding DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行建表脚本（任选一种方式）：

```bash
mysql -u root -p demo_sharding < backend/src/main/resources/sql/init.sql
```

或在客户端中打开 **`backend/src/main/resources/sql/init.sql`** 执行。

3. 修改 **`backend/src/main/resources/sharding.yaml`** 中 **`jdbcUrl` / `username` / `password`**，与本地 MySQL 一致。

### 2. 后端

```bash
cd backend
export JAVA_HOME=（你的 JDK 21 路径）
mvn spring-boot:run
```

- 端口：`http://127.0.0.1:8080`
- 健康检查：`GET /api/health`
- 订单分页：`GET /api/orders?pageNum=1&pageSize=10`，可选 `userId`  
  响应：`{ total, pageNum, pageSize, pages, list }`

IDE 请将 **`backend`** 作为 Maven 工程根目录打开（含 `pom.xml`）。

### 3. 前端

```bash
cd frontend
npm install
npm run dev
```

默认通过 Vite 代理访问 `/api`。

---

## 学习路径（建议顺序）

按下面顺序读代码、跑接口、对照日志，再扩展到官方进阶主题。

### 第一阶段：概念与接入

1. **为什么需要中间件**：单表过大 → 拆成多张**物理表**；应用仍希望只写一张**逻辑表**，由中间件做路由与改写。
2. **读 `application.yml`**：`spring.datasource` 使用 **`org.apache.shardingsphere.driver.ShardingSphereDriver`**，`url` 指向 **`jdbc:shardingsphere:classpath:sharding.yaml`**。
3. **读官方概念**：[数据分片](https://shardingsphere.apache.org/document/current/en/features/sharding/)、[核心概念（表、分片键、分片算法）](https://shardingsphere.apache.org/document/current/en/features/sharding/concept/)。

### 第二阶段：分片配置（本仓库核心）

1. **读 `sharding.yaml`**：  
   - **`dataSources`**：真实 JDBC 连接（本例单数据源 `ds_0`）。  
   - **`rules: !SHARDING`** → **`autoTables.t_order`**：自动映射物理表名。  
   - **`shardingAlgorithms`**：`BOUNDARY_RANGE` 与 **`sharding-ranges`**。  
   - **`keyGenerators.snowflake`**：分布式主键。  
   - **`props.sql-show`**：打印逻辑 SQL 与改写结果。
2. **对照常量**：`com.example.shardingdemo.sharding.OrderShardingConstants` 与 YAML 边界一致，用于预览接口与前端展示。

### 第三阶段：业务 SQL 与运行时现象

1. **读 `mapper/OrderMapper.xml`**：表名一律为逻辑表 **`t_order`**，不要写 `t_order_0`。
2. **启动后操作**：插入订单、列表分页、带/不带 `user_id` 查询；观察控制台 **路由到哪些表**、跨分片时 **如何合并**。
3. **分页**：使用 PageHelper；跨分片 + 排序 + 深分页成本高，体会「尽量带分片键」的意义。

### 第四阶段：工程化与其它主题

1. **时间**：订单 `createTime` 使用 **`java.time.Instant`**（UTC 语义）；JDBC 连接 `serverTimezone=UTC`；JSON 见 `JacksonConfig` 与下文「时间与 JSON」。
2. **进阶（官方后续学习）**  
   - [读写分离](https://shardingsphere.apache.org/document/current/en/features/readwrite-splitting/)  
   - [ShardingSphere-Proxy](https://shardingsphere.apache.org/document/current/en/user-manual/shardingsphere-proxy/)（对应用透明、多语言）  
   - [分布式事务](https://shardingsphere.apache.org/document/current/en/features/transaction/)（与 Spring Boot 3 / XA 的兼容性以文档为准）  
   - [弹性伸缩 / 迁移](https://shardingsphere.apache.org/document/current/en/features/sharding/scaling/)  
   - [DistSQL](https://shardingsphere.apache.org/document/current/en/user-manual/shardingsphere-proxy/distsql/)（在线改规则）  
   - 分库分表、广播表、绑定表等见 [用户手册目录](https://shardingsphere.apache.org/document/current/en/user-manual/)。

---

## 分片规则摘要（与 `sharding.yaml` 一致）

- **`autoTables`** + **`BOUNDARY_RANGE`**，`sharding-ranges: 1000000000` → 两区间对应 **`t_order_0`**、**`t_order_1`**。
- 扩容：增加边界与物理表、做数据迁移；或评估 **`VOLUME_RANGE`** 等算法（见官方[内置分片算法](https://shardingsphere.apache.org/document/current/en/user-manual/common-config/builtin-algorithm/sharding/)）。

---

## 时间与 JSON

- **排序**：`ORDER BY create_time DESC, id DESC`。
- **存库**：`create_time` 为 **`TIMESTAMP(3)`**，JDBC URL 使用 **`serverTimezone=UTC`**，与 **`Instant`** 的 UTC 时刻一致。
- **领域模型**：`Order.createTime` 为 **`Instant`**；业务写入用 **`Instant.now()`**。
- **JSON**：`Instant` 经 **`JacksonConfig`** 序列化为东八区墙钟字符串 **`yyyy-MM-dd HH:mm:ss`**（如 `2026-03-28 23:24:24`）；反序列化将同一格式按 **Asia/Shanghai** 解析为 **`LocalDateTime` → `ZonedDateTime` → `Instant`**。其它 `LocalDateTime` / `LocalDate` / `LocalTime` 仍走全局 formatter。
- **已有库升级**：若表仍为旧版 **`DATETIME`**，请执行 **`ALTER TABLE ... MODIFY create_time TIMESTAMP(3) ...`** 或按 `init.sql` 重建表，避免与 `Instant` 映射不一致。

---

## 若 Maven 编译报「找不到 Spring/MyBatis 包」

多见于本机 **`~/.m2/settings.xml`** 里 **`localRepository` 路径异常**。可尝试：

```bash
cd backend
mvn -Dmaven.repo.local=$HOME/.m2/repository clean compile
```

并修正 `localRepository` 为合法绝对路径。
