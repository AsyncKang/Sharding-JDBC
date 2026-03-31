package com.example.shardingdemo.dto;

import com.github.pagehelper.PageInfo;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 统一分页响应（避免直接暴露 PageInfo 的全部字段）。
 */
public record PageResult<T>(long total, int pageNum, int pageSize, int pages, List<T> list) {

    public static <T> PageResult<T> of(PageInfo<T> pageInfo) {
        return new PageResult<>(
                pageInfo.getTotal(),
                pageInfo.getPageNum(),
                pageInfo.getPageSize(),
                pageInfo.getPages(),
                pageInfo.getList());
    }

    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(
                page.getTotalElements(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalPages(),
                page.getContent());
    }
}
