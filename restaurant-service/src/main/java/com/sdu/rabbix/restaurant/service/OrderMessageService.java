package com.sdu.rabbix.restaurant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbix.common.domain.dto.OrderMessageDTO;
import com.sdu.rabbix.transaction.listener.AbstractMessageListener;
import com.sdu.rabbix.transaction.transmitter.TransMessageTransmitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service("OrderMessageService")
@Slf4j
public class OrderMessageService extends AbstractMessageListener {

    @Value("${rabbitmq.exchange.order-restaurant}")
    private String orderRestaurantExchange;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    @Resource
    private TransMessageTransmitter transmitter;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void receiveMessage(Message message) throws Exception {
        OrderMessageDTO orderMessage = objectMapper.readValue(message.getBody(), OrderMessageDTO.class);
        log.info("当前订单状态: {}", orderMessage.getOrderStatus());
        // 根据产品id从数据库获取到订单中的产品
        orderMessage.setConfirmed(true);
        // 确认无误后，将消息回发给订单服务
        log.info("餐厅服务发送给订单服务: {}", orderMessage);
        transmitter.send(orderRestaurantExchange, orderRoutingKey, orderMessage);
    }
}
