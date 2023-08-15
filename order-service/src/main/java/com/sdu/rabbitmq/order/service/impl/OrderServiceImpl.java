package com.sdu.rabbitmq.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sdu.rabbitmq.common.response.ResponseResult;
import com.sdu.rabbitmq.common.response.exception.BusinessException;
import com.sdu.rabbitmq.common.response.exception.ExceptionEnum;
import com.sdu.rabbitmq.order.entity.po.Product;
import com.sdu.rabbitmq.order.mapper.ProductMapper;
import com.sdu.rabbitmq.order.service.OrderService;
import com.sdu.rabbitmq.rdts.transmitter.TransMessageTransmitter;
import com.sdu.rabbitmq.order.enums.OrderStatus;
import com.sdu.rabbitmq.order.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.order.entity.po.OrderDetail;
import com.sdu.rabbitmq.order.entity.vo.CreateOrderVO;
import com.sdu.rabbitmq.order.repository.OrderDetailMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Service("OrderService")
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Resource
    private ProductMapper productMapper;

    @Value("${rabbitmq.exchange.order-restaurant}")
    private String exchangeOrderRestaurant;

    @Value("${rabbitmq.restaurant-routing-key}")
    public String restaurantRoutingKey;

    @Resource
    private TransMessageTransmitter transmitter;

    public ResponseResult createOrder(CreateOrderVO createOrderVO) {
        log.info("createOrder:orderCreateVO: {}", createOrderVO);
        // 判断是否有库存
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", createOrderVO.getProductId());
        Product product = productMapper.selectOne(queryWrapper);
        if (null == product) {
            throw new BusinessException(ExceptionEnum.NO_PRODUCT);
        }
        if (product.getStockLocked() >= product.getStock()) {
            throw new BusinessException(ExceptionEnum.NO_STOCK);
        }
        // 锁定库存
        int lockStatus = productMapper.lockStock(createOrderVO.getProductId());
        if (lockStatus <= 0) {
            throw new BusinessException(ExceptionEnum.NO_STOCK);
        }
        // 商品存在且库存足够 创建订单 设置订单状态为创建中
        OrderDetail order = new OrderDetail();
        order.setAddress(createOrderVO.getAddress());
        order.setProductId(createOrderVO.getProductId());
        order.setAccountId(createOrderVO.getAccountId());
        order.setCreateTime(new Date());
        order.setStatus(OrderStatus.WAITING_PAY);
        orderDetailMapper.insert(order);

        // 创建消息队列传输对象
        OrderMessageDTO orderMessage = new OrderMessageDTO();
        orderMessage.setOrderId(order.getId());
        orderMessage.setProductId(order.getProductId());
        orderMessage.setAccountId(order.getAccountId());
        orderMessage.setOrderStatus(OrderStatus.WAITING_PAY);
        // 将订单信息发送到延迟队列 等待支付
        try {
            transmitter.send(exchangeOrderRestaurant, restaurantRoutingKey, orderMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        log.info("Order service message sent!");
        return ResponseResult.ok();
    }
}
