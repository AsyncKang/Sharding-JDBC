package com.example.shardingdemo.dto;

/**
 * 与 {@code sharding.yaml} 分库分表规则一致的推导结果（教学预览，非 ShardingSphere 运行时 API）。
 */
public record ShardPreview(
        long userId,
        String dataSource,
        String physicalTable,
        String actualDataNode,
        String ruleSummary
) {}
