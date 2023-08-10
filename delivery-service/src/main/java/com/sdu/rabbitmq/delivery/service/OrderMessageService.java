package com.sdu.rabbitmq.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.delivery.enums.DeliverymanStatus;
import com.sdu.rabbitmq.delivery.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.delivery.entity.po.Deliveryman;
import com.sdu.rabbitmq.delivery.repository.DeliverymanMapper;
import com.sdu.rabbitmq.rdts.listener.AbstractMessageListener;
import com.sdu.rabbitmq.rdts.transmitter.TransMessageTransmitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Service("OrderMessageService")
@Slf4j
public class OrderMessageService extends AbstractMessageListener {

    @Resource
    private DeliverymanMapper deliverymanMapper;

    @Resource
    private TransMessageTransmitter transmitter;

    @Value("${rabbitmq.exchange.order-delivery}")
    private String orderDeliveryExchange;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void receiveMessage(Message message) {
        log.info("receive message: {}", message);
        try {
            OrderMessageDTO orderMessage = objectMapper.readValue(message.getBody(), OrderMessageDTO.class);
            // 从数据库中查找状态是可配送的骑手
            QueryWrapper<Deliveryman> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", DeliverymanStatus.AVAILABLE);
            List<Deliveryman> deliverymen = deliverymanMapper.selectList(queryWrapper);
            // 随机选择一个空闲的骑手，将该订单分配给该骑手
            orderMessage.setDeliverymanId(deliverymen.get(0).getId());
            // 将信息发送给订单服务，说明匹配到了骑手
            log.info("Delivery send message---OrderMessage: {}", orderMessage);
            transmitter.send(orderDeliveryExchange, orderRoutingKey, orderMessage);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
