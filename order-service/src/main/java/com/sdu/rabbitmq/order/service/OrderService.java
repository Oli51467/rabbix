package com.sdu.rabbitmq.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.sdu.rabbitmq.order.common.enums.OrderStatus;
import com.sdu.rabbitmq.order.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.order.entity.po.OrderDetail;
import com.sdu.rabbitmq.order.entity.vo.CreateOrderVO;
import com.sdu.rabbitmq.order.repository.OrderDetailMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

@Service("OrderService")
@Slf4j
public class OrderService {

    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Value("${rabbitmq.exchange.order-restaurant}")
    private String exchangeOrderRestaurant;

    @Value("${rabbitmq.exchange}")
    public String exchangeName;

    @Value("${rabbitmq.restaurant-routing-key}")
    public String restaurantRoutingKey;

    @Value("${rabbitmq.deliveryman-routing-key}")
    public String deliverymanRoutingKey;

    ObjectMapper objectMapper = new ObjectMapper();

    public void createOrder(CreateOrderVO createOrderVO) throws IOException, TimeoutException {
        log.info("createOrder:orderCreateVO: {}", createOrderVO);
        // 创建订单
        OrderDetail order = new OrderDetail();
        order.setAddress(createOrderVO.getAddress());
        order.setProductId(createOrderVO.getProductId());
        order.setAccountId(createOrderVO.getAccountId());
        order.setStatus(OrderStatus.ORDER_CREATING);
        order.setCreateTime(new Date());
        orderDetailMapper.insert(order);

        // 创建消息队列传输对象
        OrderMessageDTO orderMessage = new OrderMessageDTO();
        orderMessage.setOrderId(order.getId());
        orderMessage.setProductId(order.getProductId());
        orderMessage.setAccountId(order.getAccountId());

        // 建立连接并将订单信息发送到消息队列 给餐厅微服务发送消息
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");

        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            // 将DTO转换成Json字符串
            String messageToSend = objectMapper.writeValueAsString(orderMessage);
            channel.basicPublish(exchangeOrderRestaurant, restaurantRoutingKey, null, messageToSend.getBytes());
        }
    }
}
