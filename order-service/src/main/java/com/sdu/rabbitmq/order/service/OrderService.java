package com.sdu.rabbitmq.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.order.entity.vo.CreateOrderVO;
import com.sdu.rabbitmq.order.repository.OrderDetailMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service("OrderService")
@Slf4j
public class OrderService {

    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Resource
    RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    public String exchangeName;

    @Value("${rabbitmq.restaurant-routing-key}")
    public String restaurantRoutingKey;

    @Value("${rabbitmq.deliveryman-routing-key}")
    public String deliverymanRoutingKey;

    ObjectMapper objectMapper = new ObjectMapper();

    public void createOrder(CreateOrderVO createOrderVO) {

    }
}
