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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static com.sdu.rabbitmq.restaurant.common.constants.LOCALHOST;

@Service("OrderMessageService")
@Slf4j
public class OrderMessageService {

    ObjectMapper objectMapper = new ObjectMapper();

    @Value("${rabbitmq.exchange.order-restaurant}")
    private String exchangeOrderRestaurant;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    @Value("${rabbitmq.restaurant-queue}")
    private String restaurantQueue;

    @Value("${rabbitmq.restaurant-routing-key}")
    private String restaurantRoutingKey;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private RestaurantMapper restaurantMapper;

    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
        log.info("restaurant start listening message...");
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(LOCALHOST);
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {

            // 声明餐厅服务的监听队列
            channel.queueDeclare(restaurantQueue, true, false, false, null);

            // 声明订单微服务和餐厅微服务通信的交换机
            channel.exchangeDeclare(exchangeOrderRestaurant, BuiltinExchangeType.DIRECT, true, false, null);
            //将队列绑定在交换机上，routingKey是key.restaurant
            channel.queueBind(restaurantQueue, exchangeOrderRestaurant, restaurantRoutingKey);

            // 绑定监听回调
            channel.basicConsume(restaurantQueue, true, deliverCallback, consumerTag -> {});
            while (true) {

            }
        }
    }

    // 只会从订单微服务接收到消息
    DeliverCallback deliverCallback = (consumerTag, message) -> {
        String messageBody = new String(message.getBody());
        log.info("deliverCallback:messageBody: {}", messageBody);
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(LOCALHOST);

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
            try (Connection connection = connectionFactory.newConnection();
                 Channel channel = connection.createChannel()) {
                String messageToSend = objectMapper.writeValueAsString(orderMessage);
                channel.basicPublish(exchangeOrderRestaurant, orderRoutingKey, null, messageToSend.getBytes());
            }
        } catch (JsonProcessingException | TimeoutException e) {
            log.error(e.getMessage(), e);
        }
    };
}
