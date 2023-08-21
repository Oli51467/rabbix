package com.sdu.rabbitmq.product.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdu.rabbitmq.common.domain.po.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface ProductMapper extends BaseMapper<Product> {

    @Update("update product set stock_locked=stock_locked + #{count} where id = #{id} and stock-stock_locked >= #{count}")
    int lockStock(Long id, Integer count);

    @Update("update product set stock_locked = stock_locked - #{count} where id = #{id}")
    int unlockStock(Long id, Integer count);

    @Update("update product set stock_locked = stock_locked - #{count}, stock = stock - #{count} where id = #{id} and stock_locked - #{count} >= 0 and stock = stock - #{count} >= 0")
    int deductStock(Long id, Integer count);
}
