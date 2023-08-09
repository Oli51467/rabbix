package com.sdu.rabbitmq.restaurant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import com.sdu.rabbitmq.restaurant.common.enums.ProductStatus;
import com.sdu.rabbitmq.restaurant.common.enums.RestaurantStatus;
import com.sdu.rabbitmq.restaurant.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.restaurant.entity.po.Product;
import com.sdu.rabbitmq.restaurant.entity.po.Restaurant;
import com.sdu.rabbitmq.restaurant.repository.ProductMapper;
import com.sdu.rabbitmq.restaurant.repository.RestaurantMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service("OrderMessageService")
@Slf4j
public class OrderMessageService {

    ObjectMapper objectMapper = new ObjectMapper();

    @Value("${rabbitmq.exchange.order-restaurant}")
    private String orderRestaurantExchange;

    @Value("${rabbitmq.exchange.dlx}")
    private String dlxExchange;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    @Value("${rabbitmq.restaurant-queue}")
    private String restaurantQueue;

    @Value("${rabbitmq.dlx-queue}")
    private String dlxQueue;

    @Value("${rabbitmq.restaurant-routing-key}")
    private String restaurantRoutingKey;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private RestaurantMapper restaurantMapper;

    @Autowired
    private Channel channel;

    @Async
    public void handleMessage() throws IOException {
        log.info("restaurant start listening message...");
        // 设置队列TTL
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 15000);
        args.put("x-dead-letter-exchange", dlxExchange);
        // 声明餐厅服务的监听队列
        channel.queueDeclare(restaurantQueue, true, false, false, args);

        // 声明订单微服务和餐厅微服务通信的交换机
        channel.exchangeDeclare(orderRestaurantExchange, BuiltinExchangeType.DIRECT, true, false, null);
        // 将队列绑定在交换机上，routingKey是key.restaurant
        channel.queueBind(restaurantQueue, orderRestaurantExchange, restaurantRoutingKey);

        // 声明死信队列通信交换机
        channel.exchangeDeclare(dlxExchange, BuiltinExchangeType.TOPIC, true, false, null);
        // 声明接收死信的队列
        channel.queueDeclare(dlxQueue, true, false, false, null);
        // 绑定死信队列和死信交换机
        channel.queueBind(dlxQueue, dlxExchange, "#");

        // 设置管道的QoS 最多只能从队列中拿5条消息来消费
        channel.basicQos(5);
        // 绑定监听回调
        channel.basicConsume(restaurantQueue, false, deliverCallback, consumerTag -> {
        });
        while (true) {
        }
    }

    // 只会从订单微服务接收到消息
    DeliverCallback deliverCallback = (consumerTag, message) -> {
        String messageBody = new String(message.getBody());
        log.info("deliverCallback:messageBody: {}", messageBody);

        try {
            // 解析Json数据
            OrderMessageDTO orderMessage = objectMapper.readValue(messageBody, OrderMessageDTO.class);
            // 根据产品id从数据库获取到订单中的产品
            Product product = productMapper.selectById(orderMessage.getProductId());
            log.info("Restaurant onMessage---product info: {}", product);
            // 根据产品中的餐厅id获取到餐厅信息
            Restaurant restaurant = restaurantMapper.selectById(product.getRestaurantId());
            log.info("Restaurant onMessage---restaurant info: {}", restaurant);
            // 确定商品没有下架并且餐厅正常营业
            if (product.getStatus() == ProductStatus.AVAILABLE && restaurant.getStatus() == RestaurantStatus.OPEN) {
                orderMessage.setConfirmed(true);
                orderMessage.setPrice(product.getPrice());
            } else {
                orderMessage.setConfirmed(false);
            }
            log.info("Restaurant send message---OrderMessage: {}", orderMessage);
            // 确认无误后，将消息回发给订单服务
            // 消息返回机制, 检查是否被正确路由
            /*channel.addReturnListener(new ReturnListener() {
                @Override
                public void handleReturn(int replyCode, String replyText, String exchange, String routingKey, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
                    log.error("Message return because of routing, replyCode: {}, replyText: {}, " +
                            "exchange: {}, routingKey: {}, body: {}", replyCode, replyText, exchange, routingKey, new String(bytes));
                }
            });
            channel.addReturnListener(new ReturnCallback() {
                @Override
                public void handle(Return returnMessage) {
                    log.error("Message return because of routing, return message: {}", returnMessage);
                }
            });*/

            // 消费端手动消息确认
            channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
            String messageToSend = objectMapper.writeValueAsString(orderMessage);
            channel.basicPublish(orderRestaurantExchange, orderRoutingKey, true, null, messageToSend.getBytes());
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
    };
}
