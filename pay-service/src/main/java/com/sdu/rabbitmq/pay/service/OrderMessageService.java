package com.sdu.rabbitmq.pay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.common.domain.dto.OrderMessageDTO;
import com.sdu.rabbitmq.rdts.listener.AbstractMessageListener;
import com.sdu.rabbitmq.rdts.transmitter.TransMessageTransmitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.io.IOException;

@Service("OrderMessageService")
@Slf4j
public class OrderMessageService extends AbstractMessageListener {

    @Resource
    private TransMessageTransmitter transmitter;

    @Value("${rabbitmq.exchange.order-settlement}")
    private String orderSettlementExchange;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void receiveMessage(Message message) throws IOException {
        OrderMessageDTO orderMessage = objectMapper.readValue(message.getBody(), OrderMessageDTO.class);
        transmitter.send(orderSettlementExchange, orderRoutingKey, orderMessage);
    }
}
