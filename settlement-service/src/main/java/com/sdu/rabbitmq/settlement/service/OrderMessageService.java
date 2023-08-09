package com.sdu.rabbitmq.settlement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.settlement.enums.SettlementStatus;
import com.sdu.rabbitmq.settlement.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.settlement.entity.po.Settlement;
import com.sdu.rabbitmq.settlement.repository.SettlementMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.Date;

import static com.sdu.rabbitmq.settlement.config.RabbitConfig.sendToRabbit;
import static com.sdu.rabbitmq.settlement.util.SnowUtil.getSnowflakeNextId;

@Service("OrderMessageService")
@Slf4j
public class OrderMessageService {

    @Resource
    private SettlementMapper settlementMapper;

    @Value("${rabbitmq.exchange.settlement-order}")
    private String orderSettlementSendExchange;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    ObjectMapper objectMapper = new ObjectMapper();

    public void handleMessage(OrderMessageDTO orderMessage) {
        log.info("Order Service received: {}", orderMessage);
        log.info("Current order status: {}", orderMessage.getOrderStatus());
        try {
            Settlement settlement = new Settlement();
            settlement.setAmount(orderMessage.getPrice());
            settlement.setCreateTime(new Date());
            settlement.setOrderId(orderMessage.getOrderId());
            settlement.setStatus(SettlementStatus.SUCCESS);
            settlement.setTransactionId(getSnowflakeNextId());
            settlementMapper.insert(settlement);
            orderMessage.setSettlementId(settlement.getId());
            log.info("settlement send---orderMessage: {}", orderMessage);
            String messageToSend = objectMapper.writeValueAsString(orderMessage);
            sendToRabbit(orderSettlementSendExchange, orderRoutingKey, messageToSend);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }
}
