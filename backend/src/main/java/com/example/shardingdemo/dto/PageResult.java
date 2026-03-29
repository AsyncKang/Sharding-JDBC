package com.example.shardingdemo.dto;

import com.github.pagehelper.PageInfo;

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
}
