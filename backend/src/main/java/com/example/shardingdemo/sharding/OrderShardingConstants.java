package com.example.shardingdemo.sharding;

/**
 * 与 {@code sharding.yaml} 中 BOUNDARY_RANGE 的 {@code sharding-ranges} 首边界一致，供预览与文档对照。
 * 修改分片边界时请同步改 YAML、此处常量及前端 {@code frontend/src/App.vue}。
 */
public final class OrderShardingConstants {

    /** user_id &lt; 该值 → 逻辑分区 0（物理表 t_order_0）；否则 → 分区 1（t_order_1） */
    public static final long USER_ID_BOUNDARY = 1_000_000_000L;

    private OrderShardingConstants() {}
}
