package com.sdu.rabbitmq.order.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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

import static com.sdu.rabbitmq.order.common.constants.LOCALHOST;

/**
 * 和rabbitmq消息处理相关的通信服务类
 */
@Service("OrderMessageService")
@Slf4j
public class OrderMessageService {

    @Value("${rabbitmq.exchange}")
    public String exchangeName;

    @Value("${rabbitmq.exchange.order-restaurant}")
    private String exchangeOrderRestaurant;

    @Value("${rabbitmq.exchange.order-delivery}")
    private String exchangeOrderDelivery;

    @Value("${rabbitmq.exchange.order-settlement}")
    private String sendExchangeOrderSettlement;

    @Value("${rabbitmq.exchange.settlement-order}")
    private String receiveExchangeOrderSettlement;

    @Value("${rabbitmq.exchange.order-reward}")
    private String exchangeOrderReward;

    @Value("${rabbitmq.order-queue}")
    private String orderQueue;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    @Value("${rabbitmq.delivery-routing-key}")
    public String deliveryRoutingKey;

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
        log.info("order service start listening message");
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(LOCALHOST);

        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {

            // 声明订单微服务的监听队列
            channel.queueDeclare(orderQueue, true, false, false, null);

            // 声明订单微服务和餐厅微服务通信的交换机
            channel.exchangeDeclare(exchangeOrderRestaurant, BuiltinExchangeType.DIRECT, true, false, null);
            // 将队列绑定在交换机上，routingKey是key.order
            channel.queueBind(orderQueue, exchangeOrderRestaurant, orderRoutingKey);

            // 声明订单微服务和骑手微服务通信的交换机
            channel.exchangeDeclare(exchangeOrderDelivery, BuiltinExchangeType.DIRECT, true, false, null);
            // 将队列绑定在交换机上,routingKey是key.order
            channel.queueBind(orderQueue, exchangeOrderDelivery, orderRoutingKey);

            // 声明订单微服务和结算微服务通信的交换机
            channel.exchangeDeclare(sendExchangeOrderSettlement, BuiltinExchangeType.FANOUT, true, false, null);
            // 将队列绑定在交换机上,routingKey是key.order
            channel.queueBind(orderQueue, receiveExchangeOrderSettlement, orderRoutingKey);

            // 声明订单微服务和积分微服务通信的交换机
            channel.exchangeDeclare(exchangeOrderReward, BuiltinExchangeType.TOPIC, true, false, null);
            // 将队列绑定在交换机上,routingKey是key.order
            channel.queueBind(orderQueue, exchangeOrderReward, orderRoutingKey);

            // 绑定监听回调
            channel.basicConsume(orderQueue, true, deliverCallback, consumerTag -> {
            });
            while (true) {
                // 消息确认
                // 是否被路由：消息返回机制
                // 消费端限流
                // 消费端消费确认
                // 消息过期机制
                // 死信队列
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
        connectionFactory.setHost(LOCALHOST);

        try {
            // 将消息体反序列化成DTO
            OrderMessageDTO orderMessage = objectMapper.readValue(messageBody, OrderMessageDTO.class);
            // 新建一个连接
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel();
            // 通过订单状态判断是哪个微服务发来的消息
            switch (orderMessage.getOrderStatus()) {
                // 订单刚创建商家还未确认 是商家发来的消息
                case ORDER_CREATING:
                    // 商家已确认订单并将价格写入
                    if (orderMessage.getConfirmed() && null != orderMessage.getPrice()) {
                        UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
                        updateWrapper.eq("id", orderMessage.getOrderId()).set("status", OrderStatus.RESTAURANT_CONFIRMED)
                                .set("price", orderMessage.getPrice());
                        orderDetailMapper.update(null, updateWrapper);
                        // 给骑手微服务发送消息
                        orderMessage.setOrderStatus(OrderStatus.RESTAURANT_CONFIRMED);
                        // 将DTO转换成Json字符串
                        String messageToSend = objectMapper.writeValueAsString(orderMessage);
                        channel.basicPublish(exchangeOrderDelivery, deliveryRoutingKey, null, messageToSend.getBytes());
                    } else {
                        updateOrderFailed(orderMessage.getOrderId());
                    }
                    break;
                // 骑手已确认后 消息的状态还没来得及改为DELIVERYMAN_CONFIRMED，所以还是RESTAURANT_CONFIRMED
                case RESTAURANT_CONFIRMED:
                    // 判断订单已经有了确定的骑手
                    if (null != orderMessage.getDeliverymanId()) {
                        // 更新数据库的订单状态和骑手信息
                        UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
                        updateWrapper.eq("id", orderMessage.getOrderId()).set("status", OrderStatus.DELIVERYMAN_CONFIRMED)
                                .set("deliveryman_id", orderMessage.getDeliverymanId());
                        orderDetailMapper.update(null, updateWrapper);
                        orderMessage.setOrderStatus(OrderStatus.DELIVERYMAN_CONFIRMED);
                        // 向结算微服务发送一条消息 发送的方式是扇形广播
                        String messageToSend = objectMapper.writeValueAsString(orderMessage);
                        channel.basicPublish(sendExchangeOrderSettlement, settlementRoutingKey, null, messageToSend.getBytes());
                    } else {
                        // 如果没有骑手，则直接更新订单的状态为失败
                        updateOrderFailed(orderMessage.getOrderId());
                    }
                    break;
                case DELIVERYMAN_CONFIRMED:
                    // 判断订单是否已经有了结算订单的id
                    if (null != orderMessage.getSettlementId()) {
                        // 更新数据库的订单状态和结算信息
                        UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
                        updateWrapper.eq("id", orderMessage.getOrderId()).set("status", OrderStatus.SETTLEMENT_CONFIRMED)
                                .set("settlement_id", orderMessage.getSettlementId());
                        orderDetailMapper.update(null, updateWrapper);
                        orderMessage.setOrderStatus(OrderStatus.SETTLEMENT_CONFIRMED);
                        // 向积分微服务发送一条消息 发送的方式是topic
                        String messageToSend = objectMapper.writeValueAsString(orderMessage);
                        channel.basicPublish(exchangeOrderReward, rewardRoutingKey, null, messageToSend.getBytes());
                    } else {
                        // 如果没有结算id，则直接更新订单的状态为失败
                        updateOrderFailed(orderMessage.getOrderId());
                    }
                    break;
                case SETTLEMENT_CONFIRMED:
                    // 判断订单是否已经有了积分的id
                    if (null != orderMessage.getRewardId()) {
                        // 更新数据库的订单状态和积分信息
                        UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
                        updateWrapper.eq("id", orderMessage.getOrderId()).set("status", OrderStatus.ORDER_CREATED)
                                .set("reward_id", orderMessage.getRewardId());
                        orderDetailMapper.update(null, updateWrapper);
                    } else {
                        // 如果没有积分id，则直接更新订单的状态为失败
                        updateOrderFailed(orderMessage.getOrderId());
                    }
                    break;
                default:
                    break;
            }
        } catch (JsonProcessingException | TimeoutException e) {
            log.error(e.getMessage(), e);
        }
    };

    private void updateOrderFailed(Long orderId) {
        UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", orderId).set("status", OrderStatus.FAILED);
        orderDetailMapper.update(null, updateWrapper);
    }
}
