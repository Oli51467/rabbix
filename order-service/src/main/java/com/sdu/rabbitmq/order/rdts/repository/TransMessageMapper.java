package com.sdu.rabbitmq.order.rdts.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdu.rabbitmq.order.rdts.domain.TransMessage;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface TransMessageMapper extends BaseMapper<TransMessage> {
}
