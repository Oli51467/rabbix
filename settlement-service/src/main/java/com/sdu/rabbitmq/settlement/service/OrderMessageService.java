package com.sdu.rabbitmq.settlement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import com.sdu.rabbitmq.settlement.common.enums.SettlementStatus;
import com.sdu.rabbitmq.settlement.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.settlement.entity.po.Settlement;
import com.sdu.rabbitmq.settlement.repository.SettlementMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.io.IOException;
import java.util.Date;

import static com.sdu.rabbitmq.settlement.util.SnowUtil.getSnowflakeNextId;

@Service("OrderMessageService")
@Slf4j
public class OrderMessageService {

    @Resource
    private SettlementMapper settlementMapper;

    @Value("${rabbitmq.exchange.order-settlement}")
    private String orderSettlementReceiveExchange;

    @Value("${rabbitmq.exchange.settlement-order}")
    private String orderSettlementSendExchange;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    @Value("${rabbitmq.settlement-routing-key}")
    private String settlementRoutingKey;

    @Value("${rabbitmq.settlement-queue}")
    private String settlementQueue;

    @Autowired
    private Channel channel;

    ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public void handleMessage() throws IOException {
        log.info("Settlement service start listening message");
        // 声明结算微服务的监听队列
        channel.queueDeclare(settlementQueue, true, false, false, null);

        // 声明订单微服务和结算微服务通信的交换机
        channel.exchangeDeclare(orderSettlementSendExchange, BuiltinExchangeType.FANOUT, true, false, null);
        // 将队列绑定在交换机上,routingKey是key.settlement
        channel.queueBind(settlementQueue, orderSettlementReceiveExchange, settlementRoutingKey);

        // 绑定监听回调
        channel.basicConsume(settlementQueue, true, deliverCallback, consumerTag -> {
        });
        while (true) {

        }
    }

    DeliverCallback deliverCallback = (consumerTag, message) -> {
        String messageBody = new String(message.getBody());
        log.info("settlement onMessage---messageBody: {}", messageBody);
        try {
            OrderMessageDTO orderMessage = objectMapper.readValue(messageBody, OrderMessageDTO.class);
            log.info("Settlement handle completed---orderMessage: {}", orderMessage);
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
            channel.basicPublish(orderSettlementSendExchange, orderRoutingKey, null, messageToSend.getBytes());
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
    };
}
