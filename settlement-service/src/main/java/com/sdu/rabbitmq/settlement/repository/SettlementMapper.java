package com.sdu.rabbitmq.settlement.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdu.rabbitmq.settlement.entity.po.Settlement;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface SettlementMapper extends BaseMapper<Settlement> {
}
