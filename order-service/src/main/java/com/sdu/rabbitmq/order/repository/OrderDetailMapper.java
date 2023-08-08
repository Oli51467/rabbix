package com.sdu.rabbitmq.order.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdu.rabbitmq.order.entity.po.OrderDetail;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

}
