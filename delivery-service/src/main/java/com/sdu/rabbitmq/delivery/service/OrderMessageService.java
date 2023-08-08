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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static com.sdu.rabbitmq.delivery.common.constants.LOCALHOST;

@Service("OrderMessageService")
@Slf4j
public class OrderMessageService {

    @Resource
    private DeliverymanMapper deliverymanMapper;

    ObjectMapper objectMapper = new ObjectMapper();

    @Value("${rabbitmq.exchange.order-delivery}")
    private String exchangeOrderDelivery;

    @Value("${rabbitmq.delivery-routing-key}")
    private String deliveryRoutingKey;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    @Value("${rabbitmq.delivery-queue}")
    private String deliveryQueue;

    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
        log.info("delivery service start listening message");
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(LOCALHOST);
        try (Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel()) {

            // 声明骑手服务的监听队列
            channel.queueDeclare(deliveryQueue, true, false, false, null);

            // 声明订单微服务和骑手微服务通信的交换机
            channel.exchangeDeclare(exchangeOrderDelivery, BuiltinExchangeType.DIRECT, true, false, null);
            //将队列绑定在交换机上，routingKey是key.restaurant
            channel.queueBind(deliveryQueue, exchangeOrderDelivery, deliveryRoutingKey);

            // 绑定监听回调
            channel.basicConsume(deliveryQueue, true, deliverCallback, consumerTag -> {});
            while (true) {

            }
        }
    }

    DeliverCallback deliverCallback = (consumerTag, message) -> {
        String messageBody = new String(message.getBody());
        log.info("Deliver onMessage---messageBody: {}", messageBody);
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(LOCALHOST);

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
            try (Connection connection = connectionFactory.newConnection();
                 Channel channel = connection.createChannel()) {
                String messageToSend = objectMapper.writeValueAsString(orderMessage);
                channel.basicPublish(exchangeOrderDelivery, orderRoutingKey, null, messageToSend.getBytes());
            }
        } catch (JsonProcessingException | TimeoutException e) {
            log.error(e.getMessage(), e);
        }
    };
}
