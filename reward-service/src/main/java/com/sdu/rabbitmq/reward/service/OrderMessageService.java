package com.sdu.rabbitmq.reward.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.rdts.listener.AbstractMessageListener;
import com.sdu.rabbitmq.rdts.transmitter.TransMessageTransmitter;
import com.sdu.rabbitmq.reward.enums.RewardStatus;
import com.sdu.rabbitmq.reward.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.reward.entity.po.Reward;
import com.sdu.rabbitmq.reward.repository.RewardMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;

@Service("OrderMessageService")
@Slf4j
public class OrderMessageService extends AbstractMessageListener {

    @Resource
    private RewardMapper rewardMapper;

    @Resource
    private TransMessageTransmitter transmitter;

    @Value("${rabbitmq.exchange.order-reward}")
    private String orderRewardExchange;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void receiveMessage(Message message) {
        log.info("receive message: {}", message);
        try {
            OrderMessageDTO orderMessage = objectMapper.readValue(message.getBody(), OrderMessageDTO.class);
            log.info("Current order status: {}", orderMessage.getOrderStatus());
            Reward reward = new Reward();
            reward.setOrderId(orderMessage.getOrderId());
            reward.setStatus(RewardStatus.SUCCESS);
            reward.setAmount(orderMessage.getPrice());
            reward.setCreateTime(new Date());
            rewardMapper.insert(reward);
            orderMessage.setRewardId(reward.getId());
            // 将消息回发给订单服务
            log.info("Reward send---orderMessage: {}", orderMessage);
            transmitter.send(orderRewardExchange, orderRoutingKey, orderMessage);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
