package com.example.shardingdemo.sharding;

/**
 * 与 {@code sharding.yaml} 一致：分库（INLINE {@code user_id % 2}）+ 分表（INLINE，语义同原 BOUNDARY_RANGE 单边界）。
 * 修改分片边界时请同步改 YAML、此处常量及前端 {@code frontend/src/App.vue}。
 */
public final class OrderShardingConstants {

    /** 分库数量，与 {@code ds_0}…{@code ds_{n-1}} 及 INLINE 表达式一致 */
    public static final int DATABASE_COUNT = 2;

    /** user_id &lt; 该值 → 表后缀 0（t_order_0）；否则 → 1（t_order_1） */
    public static final long USER_ID_BOUNDARY = 1_000_000_000L;

    /** 与 Groovy {@code user_id % DATABASE_COUNT} 对齐（演示场景建议 user_id ≥ 0） */
    public static int databaseSuffix(long userId) {
        return Math.floorMod(userId, DATABASE_COUNT);
    }

    /** 与分表 INLINE 表达式 {@code user_id < USER_ID_BOUNDARY ? 0 : 1} 一致 */
    public static int tableSuffix(long userId) {
        return userId < USER_ID_BOUNDARY ? 0 : 1;
    }

    private OrderShardingConstants() {}
}
