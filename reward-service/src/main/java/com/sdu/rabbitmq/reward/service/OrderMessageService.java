package com.sdu.rabbitmq.reward.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import com.sdu.rabbitmq.reward.common.enums.RewardStatus;
import com.sdu.rabbitmq.reward.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.reward.entity.po.Reward;
import com.sdu.rabbitmq.reward.repository.RewardMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;

@Service("OrderMessageService")
@Slf4j
public class OrderMessageService {

    @Resource
    private RewardMapper rewardMapper;

    @Value("${rabbitmq.exchange.order-reward}")
    private String orderRewardExchange;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    @Value("${rabbitmq.reward-queue}")
    private String rewardQueue;

    @Autowired
    private Channel channel;

    ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public void handleMessage() throws IOException {
        log.info("Reward service start listening message");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 绑定监听回调
        channel.basicConsume(rewardQueue, true, deliverCallback, consumerTag -> {
        });
        while (true) {

        }
    }

    DeliverCallback deliverCallback = (consumerTag, message) -> {
        String messageBody = new String(message.getBody());
        log.info("Reward onMessage---messageBody:{}", messageBody);
        try {
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody, OrderMessageDTO.class);
            log.info("Reward handle completed---orderMessage: {}", orderMessageDTO);
            Reward reward = new Reward();
            reward.setOrderId(orderMessageDTO.getOrderId());
            reward.setStatus(RewardStatus.SUCCESS);
            reward.setAmount(orderMessageDTO.getPrice());
            reward.setCreateTime(new Date());
            rewardMapper.insert(reward);
            orderMessageDTO.setRewardId(reward.getId());
            log.info("Reward send---orderMessage: {}", orderMessageDTO);

            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
            channel.basicPublish(orderRewardExchange, orderRoutingKey, null, messageToSend.getBytes());
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
    };
}
