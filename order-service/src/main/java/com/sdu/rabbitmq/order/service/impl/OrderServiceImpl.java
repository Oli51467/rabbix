package com.sdu.rabbitmq.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.order.entity.vo.CreateOrderVO;
import com.sdu.rabbitmq.order.repository.OrderDetailDAO;
import com.sdu.rabbitmq.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service("OrderService")
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderDetailDAO orderDetailDAO;

    @Resource
    RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    public String exchangeName;

    @Value("${rabbitmq.restaurant-routing-key}")
    public String restaurantRoutingKey;

    @Value("${rabbitmq.deliveryman-routing-key}")
    public String deliverymanRoutingKey;

    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void createOrder(CreateOrderVO createOrderVO) {

    }
}
