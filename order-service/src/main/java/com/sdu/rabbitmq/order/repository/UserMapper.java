package com.sdu.rabbitmq.order.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdu.rabbitmq.order.entity.po.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

}
