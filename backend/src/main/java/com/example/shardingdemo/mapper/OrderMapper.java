package com.example.shardingdemo.mapper;

import com.example.shardingdemo.domain.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {

    int insert(Order order);

    Order selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<Order> selectAll();

    List<Order> selectByUserId(@Param("userId") Long userId);
}
