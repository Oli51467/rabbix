package com.sdu.rabbitmq.reward.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.reward.enums.RewardStatus;
import com.sdu.rabbitmq.reward.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.reward.entity.po.Reward;
import com.sdu.rabbitmq.reward.repository.RewardMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

import static com.sdu.rabbitmq.reward.config.RabbitConfig.sendToRabbit;

@Service("OrderMessageService")
@Slf4j
public class OrderMessageService {

    @Resource
    private RewardMapper rewardMapper;

    @Value("${rabbitmq.exchange.order-reward}")
    private String orderRewardExchange;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    ObjectMapper objectMapper = new ObjectMapper();

    public void handleMessage(OrderMessageDTO orderMessage) {
        log.info("Order Service received: {}", orderMessage);
        log.info("Current order status: {}", orderMessage.getOrderStatus());
        try {
            Reward reward = new Reward();
            reward.setOrderId(orderMessage.getOrderId());
            reward.setStatus(RewardStatus.SUCCESS);
            reward.setAmount(orderMessage.getPrice());
            reward.setCreateTime(new Date());
            rewardMapper.insert(reward);
            orderMessage.setRewardId(reward.getId());
            log.info("Reward send---orderMessage: {}", orderMessage);

            String messageToSend = objectMapper.writeValueAsString(orderMessage);
            sendToRabbit(orderRewardExchange, orderRoutingKey, messageToSend);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }
}
