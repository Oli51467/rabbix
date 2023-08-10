package com.sdu.rabbitmq.settlement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.rdts.listener.AbstractMessageListener;
import com.sdu.rabbitmq.rdts.transmitter.TransMessageTransmitter;
import com.sdu.rabbitmq.settlement.enums.SettlementStatus;
import com.sdu.rabbitmq.settlement.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.settlement.entity.po.Settlement;
import com.sdu.rabbitmq.settlement.repository.SettlementMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.io.IOException;
import java.util.Date;

import static com.sdu.rabbitmq.settlement.util.SnowUtil.getSnowflakeNextId;

@Service("OrderMessageService")
@Slf4j
public class OrderMessageService extends AbstractMessageListener {

    @Resource
    private SettlementMapper settlementMapper;

    @Resource
    private TransMessageTransmitter transmitter;

    @Value("${rabbitmq.exchange.settlement-order}")
    private String orderSettlementSendExchange;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void receiveMessage(Message message) {
        log.info("receive message: {}", message);
        try {
            OrderMessageDTO orderMessage = objectMapper.readValue(message.getBody(), OrderMessageDTO.class);
            log.info("Current order status: {}", orderMessage.getOrderStatus());
            Settlement settlement = new Settlement();
            settlement.setAmount(orderMessage.getPrice());
            settlement.setCreateTime(new Date());
            settlement.setOrderId(orderMessage.getOrderId());
            settlement.setStatus(SettlementStatus.SUCCESS);
            settlement.setTransactionId(getSnowflakeNextId());
            settlementMapper.insert(settlement);
            orderMessage.setSettlementId(settlement.getId());
            // 将消息回发给订单服务
            log.info("settlement send---orderMessage: {}", orderMessage);
            transmitter.send(orderSettlementSendExchange, orderRoutingKey, orderMessage);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
