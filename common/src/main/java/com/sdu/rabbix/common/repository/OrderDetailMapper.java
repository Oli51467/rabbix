package com.sdu.rabbix.common.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdu.rabbix.common.domain.po.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

}
