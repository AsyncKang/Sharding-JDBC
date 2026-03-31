package com.example.shardingdemo.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.example.shardingdemo.domain.Order;
import com.example.shardingdemo.dto.PageResult;
import com.example.shardingdemo.es.EsOrderDoc;
import com.example.shardingdemo.es.EsOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderEsQueryService {

    private final EsOrderRepository esOrderRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public OrderEsQueryService(
            EsOrderRepository esOrderRepository,
            ElasticsearchOperations elasticsearchOperations) {
        this.esOrderRepository = esOrderRepository;
        this.elasticsearchOperations = elasticsearchOperations;
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

    public PageResult<Order> search(
            Long userId,
            String keyword,
            Instant startTime,
            Instant endTime,
            int pageNum,
            int pageSize) {
        List<Query> mustQueries = new ArrayList<>();
        if (userId != null) {
            mustQueries.add(Query.of(q -> q.term(t -> t.field("userId").value(userId))));
        }
        if (StringUtils.hasText(keyword)) {
            mustQueries.add(Query.of(q -> q.match(m -> m.field("title").query(keyword))));
        }
        if (startTime != null || endTime != null) {
            mustQueries.add(Query.of(q -> q.range(r -> {
                var dateRange = r.field("createTime");
                if (startTime != null) {
                    dateRange.gte(JsonData.of(startTime.toString()));
                }
                if (endTime != null) {
                    dateRange.lte(JsonData.of(endTime.toString()));
                }
                return dateRange;
            })));
        }

        Query queryDsl = mustQueries.isEmpty()
                ? Query.of(q -> q.matchAll(m -> m))
                : Query.of(q -> q.bool(b -> b.must(mustQueries)));
        NativeQuery query = NativeQuery.builder()
                .withQuery(queryDsl)
                .withPageable(PageRequest.of(pageNum - 1, pageSize))
                .withSort(s -> s.field(f -> f.field("createTime").order(SortOrder.Desc)))
                .withSort(s -> s.field(f -> f.field("id").order(SortOrder.Desc)))
                .build();
        SearchHits<EsOrderDoc> searchHits = elasticsearchOperations.search(query, EsOrderDoc.class);
        List<Order> list = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toOrder)
                .toList();
        long total = searchHits.getTotalHits();
        int pages = (int) ((total + pageSize - 1) / pageSize);
        return new PageResult<>(total, pageNum, pageSize, pages, list);
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
