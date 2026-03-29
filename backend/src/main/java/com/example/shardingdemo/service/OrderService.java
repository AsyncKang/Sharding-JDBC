package com.example.shardingdemo.service;

import com.example.shardingdemo.domain.Order;
import com.example.shardingdemo.dto.PageResult;
import com.example.shardingdemo.mapper.OrderMapper;
import com.example.shardingdemo.sharding.OrderShardingConstants;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class OrderService {

    private final OrderMapper orderMapper;

    public OrderService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Order create(Long userId, String title, java.math.BigDecimal amount) {
        Order order = new Order();
        order.setUserId(userId);
        order.setTitle(title);
        order.setAmount(amount);
        order.setCreateTime(Instant.now());
        orderMapper.insert(order);
        return order;
    }

    public Order get(Long id, Long userId) {
        return orderMapper.selectByIdAndUserId(id, userId);
    }

    /**
     * 跨分片分页（无 userId）：ShardingSphere 会对各分片改写 LIMIT 并合并，深分页成本高。
     */
    public PageResult<Order> pageAll(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Order> list = orderMapper.selectAll();
        return PageResult.of(new PageInfo<>(list));
    }

    /**
     * 单分片分页（带 userId）：仅路由到一个物理表，与普通单表分页一致。
     */
    public PageResult<Order> pageByUser(Long userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Order> list = orderMapper.selectByUserId(userId);
        return PageResult.of(new PageInfo<>(list));
    }

    /**
     * 教学用：与 {@code BoundaryBasedRangeShardingAlgorithm} 单边界两区间一致。
     */
    public String previewPhysicalTable(long userId) {
        int suffix = userId < OrderShardingConstants.USER_ID_BOUNDARY ? 0 : 1;
        return "t_order_" + suffix;
    }
}
