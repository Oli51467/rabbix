package com.sdu.rabbix.order.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdu.rabbix.common.domain.po.OrderProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface OrderProductMapper extends BaseMapper<OrderProduct> {

    @Select("select * from order_product where order_id = #{id}")
    List<OrderProduct> queryByOrderId(Long id);
}
