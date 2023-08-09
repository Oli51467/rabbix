package com.sdu.rabbitmq.order.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.sdu.rabbitmq.order.common.enums.OrderStatus;
import com.sdu.rabbitmq.order.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.order.entity.po.OrderDetail;
import com.sdu.rabbitmq.order.entity.vo.CreateOrderVO;
import com.sdu.rabbitmq.order.repository.OrderDetailMapper;
import lombok.extern.slf4j.Slf4j;
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

    @Value("${rabbitmq.exchange.order-restaurant}")
    private String exchangeOrderRestaurant;

    @Value("${rabbitmq.exchange}")
    public String exchangeName;

    @Value("${rabbitmq.restaurant-routing-key}")
    public String restaurantRoutingKey;

    @Value("${rabbitmq.delivery-routing-key}")
    public String deliveryRoutingKey;

    @Autowired
    private Channel channel;

    ObjectMapper objectMapper = new ObjectMapper();

    public void createOrder(CreateOrderVO createOrderVO) throws IOException, InterruptedException {
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

        // 建立连接并将订单信息发送到消息队列 给餐厅微服务发送消息
        channel.confirmSelect();
        // 将DTO转换成Json字符串然后发送给商家微服务
        AMQP.BasicProperties properties = new AMQP.BasicProperties().builder().expiration("15000").build();
        String messageToSend = objectMapper.writeValueAsString(orderMessage);
        channel.basicPublish(exchangeOrderRestaurant, restaurantRoutingKey, null, messageToSend.getBytes());
        // 设置单条消息的过期时间
        // 单条同步发送端确认机制
        if (channel.waitForConfirms()) {
            log.info("RabbitMQ confirm send success");
        } else {
            // 发送失败后续的服务就不会收到，所以修改数据库的状态不会影响业务
            UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", order.getId()).set("status", OrderStatus.FAILED);
            orderDetailMapper.update(null, updateWrapper);
            log.error("RabbitMQ confirm send failed");
        }
    }
}
