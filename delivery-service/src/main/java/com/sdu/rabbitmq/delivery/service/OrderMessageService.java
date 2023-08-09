package com.sdu.rabbitmq.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import com.sdu.rabbitmq.delivery.common.enums.DeliverymanStatus;
import com.sdu.rabbitmq.delivery.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.delivery.entity.po.Deliveryman;
import com.sdu.rabbitmq.delivery.repository.DeliverymanMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Service("OrderMessageService")
@Slf4j
public class OrderMessageService {

    @Resource
    private DeliverymanMapper deliverymanMapper;

    @Value("${rabbitmq.exchange.order-delivery}")
    private String orderDeliveryExchange;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    @Value("${rabbitmq.delivery-queue}")
    private String deliveryQueue;

    @Autowired
    private Channel channel;

    ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public void handleMessage() throws IOException {
        log.info("delivery service start listening message");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 绑定监听回调
        channel.basicConsume(deliveryQueue, true, deliverCallback, consumerTag -> {
        });
        while (true) {

        }
    }

    DeliverCallback deliverCallback = (consumerTag, message) -> {
        String messageBody = new String(message.getBody());
        log.info("Deliver onMessage---messageBody: {}", messageBody);

        try {
            // 从数据库中查找状态是可配送的骑手
            OrderMessageDTO orderMessage = objectMapper.readValue(messageBody, OrderMessageDTO.class);
            QueryWrapper<Deliveryman> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", DeliverymanStatus.AVAILABLE);
            List<Deliveryman> deliverymen = deliverymanMapper.selectList(queryWrapper);
            // 随机选择一个空闲的骑手，将该订单分配给该骑手
            orderMessage.setDeliverymanId(deliverymen.get(0).getId());
            log.info("Delivery send message---OrderMessage: {}", orderMessage);
            // 将信息发送给订单服务，说明匹配到了骑手
            String messageToSend = objectMapper.writeValueAsString(orderMessage);
            channel.basicPublish(orderDeliveryExchange, orderRoutingKey, null, messageToSend.getBytes());
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
    };
}
