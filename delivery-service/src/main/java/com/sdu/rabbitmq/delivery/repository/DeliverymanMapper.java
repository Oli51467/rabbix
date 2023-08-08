package com.sdu.rabbitmq.delivery.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdu.rabbitmq.delivery.entity.po.Deliveryman;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface DeliverymanMapper extends BaseMapper<Deliveryman> {
}
