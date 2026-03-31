package com.example.shardingdemo.service;

import com.example.shardingdemo.domain.Order;
import com.example.shardingdemo.dto.PageResult;
import com.example.shardingdemo.es.EsOrderDoc;
import com.example.shardingdemo.es.EsOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class OrderEsQueryService {

    private final EsOrderRepository esOrderRepository;

    public OrderEsQueryService(EsOrderRepository esOrderRepository) {
        this.esOrderRepository = esOrderRepository;
    }

    public PageResult<Order> pageAll(int pageNum, int pageSize) {
        PageRequest request = PageRequest.of(pageNum - 1, pageSize, Sort.by(
                Sort.Order.desc("createTime"),
                Sort.Order.desc("id")));
        Page<EsOrderDoc> page = esOrderRepository.findAll(request);
        return PageResult.of(page.map(this::toOrder));
    }

    public PageResult<Order> pageByUser(Long userId, int pageNum, int pageSize) {
        PageRequest request = PageRequest.of(pageNum - 1, pageSize, Sort.by(
                Sort.Order.desc("createTime"),
                Sort.Order.desc("id")));
        Page<EsOrderDoc> page = esOrderRepository.findByUserId(userId, request);
        return PageResult.of(page.map(this::toOrder));
    }

    private Order toOrder(EsOrderDoc doc) {
        Order order = new Order();
        order.setId(doc.getId());
        order.setUserId(doc.getUserId());
        order.setTitle(doc.getTitle());
        order.setAmount(doc.getAmount());
        order.setCreateTime(doc.getCreateTime());
        return order;
    }
}
