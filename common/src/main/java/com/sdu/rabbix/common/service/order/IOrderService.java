package com.sdu.rabbix.common.service.order;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sdu.rabbix.common.commons.enums.OrderStatus;
import com.sdu.rabbix.common.domain.po.OrderDetail;
import com.sdu.rabbix.common.repository.OrderDetailMapper;
import com.sdu.rabbix.common.utils.SnowUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Service
public class IOrderService {

    @Resource
    private OrderDetailMapper orderDetailMapper;

    public void updateOrderStatus(Long orderId, OrderStatus orderStatus) {
        log.info("update order status to: {}, orderId: {}", orderStatus.name(), orderId);
        UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", orderId).set("status", orderStatus.name());
        orderDetailMapper.update(null, updateWrapper);
    }

    public void updateOrderStatusAndPrice(Long orderId, OrderStatus orderStatus, BigDecimal price) {
        log.info("update order status to: {}, orderId: {}", orderStatus.name(), orderId);
        UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", orderId).set("status", orderStatus.name()).set("price", price);
        orderDetailMapper.update(null, updateWrapper);
    }

    public void updateOrderStatusCreated(Long orderId, OrderStatus orderStatus, Long deliveryId, BigDecimal reward) {
        log.info("update order status to: {}, orderId: {}", orderStatus.name(), orderId);
        UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", orderId)
                .set("status", orderStatus.name()).set("reward", reward).set("deliveryman_id", deliveryId);
        orderDetailMapper.update(null, updateWrapper);
    }

    public OrderDetail queryById(Long orderId) {
        QueryWrapper<OrderDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", orderId);
        return orderDetailMapper.selectOne(queryWrapper);
    }

    public OrderDetail createOrder(String address, Long accountId, BigDecimal price) {
        OrderDetail order = new OrderDetail();
        order.setId(SnowUtil.getSnowflakeNextId());
        order.setAddress(address);
        order.setAccountId(accountId);
        order.setCreateTime(new Date());
        order.setPrice(price);
        order.setStatus(OrderStatus.WAITING_PAY);
        orderDetailMapper.insert(order);
        return order;
    }
}
