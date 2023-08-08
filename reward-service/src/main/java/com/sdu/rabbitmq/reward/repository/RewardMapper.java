package com.sdu.rabbitmq.reward.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdu.rabbitmq.reward.entity.po.Reward;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface RewardMapper extends BaseMapper<Reward> {
}
