package com.sdu.rabbitmq.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.sdu.rabbitmq.order.repository.OrderDetailMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 和rabbitmq消息处理相关的通信服务类
 */
@Service
@Slf4j
public class OrderMessageService {

    @Value("${rabbitmq.exchange}")
    public String exchangeName;

    @Value("${rabbitmq.exchange.order-restaurant}")
    private String exchangeOrderRestaurant;

    @Value("${rabbitmq.exchange.order-deliveryman}")
    private String exchangeOrderDeliveryman;

    @Value("${rabbitmq.order-queue}")
    private String orderQueue;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    @Value("${rabbitmq.deliveryman-routing-key}")
    public String deliverymanRoutingKey;

    @Value("${rabbitmq.settlement-routing-key}")
    public String settlementRoutingKey;

    @Value("${rabbitmq.reward-routing-key}")
    public String rewardRoutingKey;

    @Resource
    private OrderDetailMapper orderDetailMapper;

    ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 声明消息队列、交换机、绑定、消息的处理
     */
    public void handleMessage() throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("127.0.0.1");

        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {

            // 声明订单微服务的监听队列
            channel.queueDeclare(orderQueue, true, false, false, null);

            // 声明订单微服务和餐厅微服务通信的交换机
            channel.exchangeDeclare(exchangeOrderRestaurant, BuiltinExchangeType.DIRECT, true, false, null);
            // 将队列绑定在交换机上，routingKey是key.order
            channel.queueBind(orderQueue, exchangeOrderRestaurant, orderRoutingKey);

            // 声明订单微服务和骑手微服务通信的交换机
            channel.exchangeDeclare(exchangeOrderDeliveryman, BuiltinExchangeType.DIRECT, true,false,null);
            // 将队列绑定在交换机上,routingKey是key.order
            channel.queueBind(orderQueue, exchangeOrderDeliveryman, orderRoutingKey);
        }
    }
}
