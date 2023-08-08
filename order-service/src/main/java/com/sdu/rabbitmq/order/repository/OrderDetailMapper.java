package com.sdu.rabbitmq.order.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdu.rabbitmq.order.entity.po.OrderDetail;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

//    @Update("update order_detail set status =#{status}, address =#{address}, account_id =#{accountId}, " +
//            "product_id =#{productId}, deliveryman_id =#{deliverymanId}, settlement_id =#{settlementId}, " +
//            "reward_id =#{rewardId}, price =#{price}, date =#{date} where id=#{id}")
//    void update(OrderDetail orderDetailPO);

    @Select("SELECT id,status,address,account_id accountId, product_id productId,deliveryman_id deliverymanId," +
            "settlement_id settlementId,reward_id rewardId,price, date FROM order_detail WHERE id = #{id}")
    OrderDetail selectOrder(Integer id);
}
