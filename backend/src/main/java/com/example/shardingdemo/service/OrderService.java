package com.example.shardingdemo.service;

import com.example.shardingdemo.domain.Order;
import com.example.shardingdemo.dto.PageResult;
import com.example.shardingdemo.dto.ShardPreview;
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
    private final OrderEsQueryService orderEsQueryService;

    public OrderService(OrderMapper orderMapper, OrderEsQueryService orderEsQueryService) {
        this.orderMapper = orderMapper;
        this.orderEsQueryService = orderEsQueryService;
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
        return orderEsQueryService.pageAll(pageNum, pageSize);
    }

    public PageResult<Order> searchByEs(
            Long userId,
            String keyword,
            Instant startTime,
            Instant endTime,
            int pageNum,
            int pageSize) {
        return orderEsQueryService.search(userId, keyword, startTime, endTime, pageNum, pageSize);
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
     * 教学用：分库（INLINE）+ 分表（INLINE，等价原 BOUNDARY_RANGE 边界），与 YAML 推导一致。
     */
    public ShardPreview previewShard(long userId) {
        int ds = OrderShardingConstants.databaseSuffix(userId);
        int tb = OrderShardingConstants.tableSuffix(userId);
        String dataSource = "ds_" + ds;
        String physicalTable = "t_order_" + tb;
        String rule =
                "分库: INLINE ds_${user_id % 2} → "
                        + dataSource
                        + " ; 分表: INLINE t_order_${user_id < "
                        + OrderShardingConstants.USER_ID_BOUNDARY
                        + " → t_order_0 else t_order_1 → "
                        + physicalTable;
        return new ShardPreview(userId, dataSource, physicalTable, dataSource + "." + physicalTable, rule);
    }
}
