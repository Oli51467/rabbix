package com.sdu.rabbitmq.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.order.enums.OrderStatus;
import com.sdu.rabbitmq.order.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.order.entity.po.OrderDetail;
import com.sdu.rabbitmq.order.entity.vo.CreateOrderVO;
import com.sdu.rabbitmq.order.repository.OrderDetailMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;

@Service("OrderService")
@Slf4j
public class OrderService {

    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.order-restaurant}")
    private String exchangeOrderRestaurant;

    @Value("${rabbitmq.restaurant-routing-key}")
    public String restaurantRoutingKey;

    ObjectMapper objectMapper = new ObjectMapper();

    public void createOrder(CreateOrderVO createOrderVO) throws IOException {
        log.info("createOrder:orderCreateVO: {}", createOrderVO);
        // 创建订单 设置订单状态为创建中
        OrderDetail order = new OrderDetail();
        order.setAddress(createOrderVO.getAddress());
        order.setProductId(createOrderVO.getProductId());
        order.setAccountId(createOrderVO.getAccountId());
        order.setCreateTime(new Date());
        order.setStatus(OrderStatus.ORDER_CREATING);
        orderDetailMapper.insert(order);

        // 创建消息队列传输对象
        OrderMessageDTO orderMessage = new OrderMessageDTO();
        orderMessage.setOrderId(order.getId());
        orderMessage.setProductId(order.getProductId());
        orderMessage.setAccountId(order.getAccountId());
        orderMessage.setOrderStatus(OrderStatus.ORDER_CREATING);

        // 将订单信息发送到消息队列 给餐厅微服务发送消息
        String messageToSend = objectMapper.writeValueAsString(orderMessage);
        MessageProperties messageProperties = new MessageProperties();

        // 设置单条消息的过期时间
        messageProperties.setExpiration("15000");
        Message message = new Message(messageToSend.getBytes(), messageProperties);

        // 设置CorrelationData
        CorrelationData correlationData = new CorrelationData();
        correlationData.setId(orderMessage.getOrderId().toString());

        rabbitTemplate.send(exchangeOrderRestaurant, restaurantRoutingKey, message, correlationData);
    }
}
