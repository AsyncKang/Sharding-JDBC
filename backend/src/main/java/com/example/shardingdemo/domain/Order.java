package com.example.shardingdemo.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 逻辑表 t_order。{@link #createTime} 使用 UTC 时刻（{@link Instant}），与库中 TIMESTAMP（UTC 存储）一致；
 * REST JSON 由 {@link com.example.shardingdemo.config.JacksonConfig} 格式化为东八区 {@code yyyy-MM-dd HH:mm:ss}。
 */
public class Order {

    private Long id;
    private Long userId;
    private String title;
    private BigDecimal amount;
    private Instant createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Instant createTime) {
        this.createTime = createTime;
    }
}
