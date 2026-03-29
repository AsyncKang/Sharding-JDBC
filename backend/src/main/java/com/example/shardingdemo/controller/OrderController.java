package com.example.shardingdemo.controller;

import com.example.shardingdemo.domain.Order;
import com.example.shardingdemo.dto.PageResult;
import com.example.shardingdemo.service.OrderService;
import com.example.shardingdemo.web.PageParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody CreateOrderRequest body) {
        Order order = orderService.create(body.userId(), body.title(), body.amount());
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> get(
            @PathVariable Long id,
            @RequestParam Long userId) {
        Order order = orderService.get(id, userId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    /**
     * 分页列表。query 参数：pageNum（默认 1）、pageSize（默认 10，最大 100）、userId（可选，有则单分片查询）。
     */
    @GetMapping
    public PageResult<Order> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        int pn = PageParam.normalizePageNum(pageNum);
        int ps = PageParam.normalizePageSize(pageSize);
        if (userId != null) {
            return orderService.pageByUser(userId, pn, ps);
        }
        return orderService.pageAll(pn, ps);
    }

    @GetMapping("/preview-shard")
    public Map<String, Object> previewShard(@RequestParam long userId) {
        String physical = orderService.previewPhysicalTable(userId);
        Map<String, Object> m = new HashMap<>();
        m.put("userId", userId);
        m.put("physicalTable", physical);
        m.put("rule", "BOUNDARY_RANGE: user_id < 1_000_000_000 -> t_order_0 ; else -> t_order_1");
        return m;
    }

    public record CreateOrderRequest(Long userId, String title, BigDecimal amount) {}
}
