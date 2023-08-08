package com.sdu.rabbitmq.restaurant.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdu.rabbitmq.restaurant.entity.po.Restaurant;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RestaurantMapper extends BaseMapper<Restaurant> {
}
