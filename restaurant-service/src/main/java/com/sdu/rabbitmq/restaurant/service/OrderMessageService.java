package com.sdu.rabbitmq.restaurant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.restaurant.enums.ProductStatus;
import com.sdu.rabbitmq.restaurant.enums.RestaurantStatus;
import com.sdu.rabbitmq.restaurant.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.restaurant.entity.po.Product;
import com.sdu.rabbitmq.restaurant.entity.po.Restaurant;
import com.sdu.rabbitmq.restaurant.repository.ProductMapper;
import com.sdu.rabbitmq.restaurant.repository.RestaurantMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.sdu.rabbitmq.restaurant.config.RabbitConfig.sendToRabbit;

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

    @Value("${rabbitmq.dlx-queue}")
    private String dlxQueue;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private RestaurantMapper restaurantMapper;

    // 只会从订单微服务接收到消息
    public void handleMessage(OrderMessageDTO orderMessage) {
        log.info("Order Service received: {}", orderMessage);
        log.info("Current order status: {}", orderMessage.getOrderStatus());
        try {
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
            String messageToSend = objectMapper.writeValueAsString(orderMessage);
            sendToRabbit(orderRestaurantExchange, orderRoutingKey, messageToSend);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }
}
