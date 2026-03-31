package com.example.shardingdemo.es;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface EsOrderRepository extends ElasticsearchRepository<EsOrderDoc, Long> {

    Page<EsOrderDoc> findByUserId(Long userId, Pageable pageable);
}
