package com.sdu.rabbitmq.settlement.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdu.rabbitmq.common.domain.po.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Update("update product set stock_locked=stock_locked + 1 where id = #{id} and stock-stock_locked > 0")
    int lockStock(Long id);

    @Update("update product set stock_locked = stock_locked - 1 where id = #{id}")
    int unlockStock(Long id);

    @Update("update product set stock_locked = stock_locked - 1, stock = stock - 1 where id = #{id}")
    int deductStock(Long id);
}
