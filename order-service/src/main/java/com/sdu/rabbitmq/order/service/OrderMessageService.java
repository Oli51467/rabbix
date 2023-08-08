package com.sdu.rabbitmq.order.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import com.sdu.rabbitmq.order.common.enums.OrderStatus;
import com.sdu.rabbitmq.order.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.order.entity.po.OrderDetail;
import com.sdu.rabbitmq.order.repository.OrderDetailMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
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
    @Async
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

            // 绑定监听回调
            channel.basicConsume(orderQueue, true, deliverCallback, consumerTag -> {});
            while (true) {

            }
        }
    }

    /**
     * 从mq接收到消息的回调
     * consumerTag 消费者类型
     * message Delivery类型的消息
     */
    DeliverCallback deliverCallback = (consumerTag, message) -> {
        String messageBody = new String(message.getBody());

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("127.0.0.1");

        try {
            // 将消息体反序列化成DTO
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody, OrderMessageDTO.class);
            // 从数据库中读取订单
            QueryWrapper<OrderDetail> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("order_id", orderMessageDTO.getOrderId());
            OrderDetail orderDetail = orderDetailMapper.selectOne(queryWrapper);

            // 通过订单状态判断是哪个微服务发来的消息
            switch (orderDetail.getStatus()) {
                // 订单刚创建商家还未确认 是商家发来的消息
                case ORDER_CREATING:
                    // 商家已确认订单并将价格写入
                    if (orderMessageDTO.getConfirmed() && null != orderMessageDTO.getPrice()) {
                        orderDetail.setStatus(OrderStatus.RESTAURANT_CONFIRMED);
                        orderDetail.setPrice(orderMessageDTO.getPrice());
                        orderDetailMapper.updateById(orderDetail);
                        // 给骑手微服务发送消息
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            // 将DTO转换成Json字符串
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish(exchangeOrderDeliveryman, deliverymanRoutingKey, null, messageToSend.getBytes());
                        }
                    } else {
                        orderDetail.setStatus(OrderStatus.FAILED);
                        orderDetailMapper.updateById(orderDetail);
                    }
                    break;
                case RESTAURANT_CONFIRMED:
                    break;
                case ORDER_CREATED:
                    break;
                case DELIVERYMAN_CONFIRMED:
                    break;
                case SETTLEMENT_CONFIRMED:
                    break;
                case FAILED:
                    break;
                default:
                    break;
            }
        } catch (JsonProcessingException | TimeoutException e) {
            e.printStackTrace();
        }
    };
}
