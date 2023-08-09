package com.sdu.rabbitmq.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.delivery.enums.DeliverymanStatus;
import com.sdu.rabbitmq.delivery.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.delivery.entity.po.Deliveryman;
import com.sdu.rabbitmq.delivery.repository.DeliverymanMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static com.sdu.rabbitmq.delivery.config.RabbitConfig.sendToRabbit;

@Service("OrderMessageService")
@Slf4j
public class OrderMessageService {

    @Resource
    private DeliverymanMapper deliverymanMapper;

    @Value("${rabbitmq.exchange.order-delivery}")
    private String orderDeliveryExchange;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    ObjectMapper objectMapper = new ObjectMapper();

    public void handleMessage(OrderMessageDTO orderMessage) {
        log.info("Order Service received: {}", orderMessage);
        log.info("Current order status: {}", orderMessage.getOrderStatus());

        try {
            // 从数据库中查找状态是可配送的骑手
            QueryWrapper<Deliveryman> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", DeliverymanStatus.AVAILABLE);
            List<Deliveryman> deliverymen = deliverymanMapper.selectList(queryWrapper);
            // 随机选择一个空闲的骑手，将该订单分配给该骑手
            orderMessage.setDeliverymanId(deliverymen.get(0).getId());
            log.info("Delivery send message---OrderMessage: {}", orderMessage);
            // 将信息发送给订单服务，说明匹配到了骑手
            String messageToSend = objectMapper.writeValueAsString(orderMessage);
            sendToRabbit(orderDeliveryExchange, orderRoutingKey, messageToSend);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }
}
