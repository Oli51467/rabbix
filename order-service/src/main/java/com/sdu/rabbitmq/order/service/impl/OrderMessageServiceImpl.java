package com.sdu.rabbitmq.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.order.repository.OrderDetailDAO;
import com.sdu.rabbitmq.order.service.OrderMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service("OrderMessageService")
@Slf4j
public class OrderMessageServiceImpl implements OrderMessageService {

    @Value("${rabbitmq.exchange}")
    public String exchangeName;

    @Value("${rabbitmq.deliveryman-routing-key}")
    public String deliverymanRoutingKey;

    @Value("${rabbitmq.settlement-routing-key}")
    public String settlementRoutingKey;

    @Value("${rabbitmq.reward-routing-key}")
    public String rewardRoutingKey;

    @Resource
    private OrderDetailDAO orderDetailDAO;

    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handleMessage() {

    }
}
