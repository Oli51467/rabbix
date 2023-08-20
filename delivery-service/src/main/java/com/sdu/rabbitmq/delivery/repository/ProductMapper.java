package com.sdu.rabbitmq.delivery.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdu.rabbitmq.common.domain.po.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Update("update product set stock_locked = stock_locked - #{count} where id = #{id}")
    void unlockStock(Long id, Integer count);
}
