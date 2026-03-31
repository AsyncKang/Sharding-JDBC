package com.example.shardingdemo.controller;

import com.example.shardingdemo.domain.Order;
import com.example.shardingdemo.dto.PageResult;
import com.example.shardingdemo.dto.ShardPreview;
import com.example.shardingdemo.service.OrderService;
import com.example.shardingdemo.web.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;

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

    @GetMapping("/es")
    public PageResult<Order> searchByEs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        int pn = PageParam.normalizePageNum(pageNum);
        int ps = PageParam.normalizePageSize(pageSize);
        return orderService.searchByEs(userId, keyword, startTime, endTime, pn, ps);
    }

    @GetMapping("/preview-shard")
    public ShardPreview previewShard(@RequestParam long userId) {
        return orderService.previewShard(userId);
    }

    public record CreateOrderRequest(Long userId, String title, BigDecimal amount) {}
}
