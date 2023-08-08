package com.sdu.rabbitmq.reward.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import com.sdu.rabbitmq.reward.common.enums.RewardStatus;
import com.sdu.rabbitmq.reward.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.reward.entity.po.Reward;
import com.sdu.rabbitmq.reward.repository.RewardMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import static com.sdu.rabbitmq.reward.common.constants.LOCALHOST;

@Service("OrderMessageService")
@Slf4j
public class OrderMessageService {

    @Resource
    private RewardMapper rewardMapper;

    @Value("${rabbitmq.exchange.order-reward}")
    private String exchangeOrderReward;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    @Value("${rabbitmq.reward-routing-key}")
    private String rewardRoutingKey;

    @Value("${rabbitmq.reward-queue}")
    private String rewardQueue;

    ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public void handleMessage() throws IOException, TimeoutException {
        log.info("Reward service start listening message");
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(LOCALHOST);
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            // 声明积分微服务的监听队列
            channel.queueDeclare(rewardQueue, true, false, false, null);

            // 声明订单微服务和积分微服务通信的交换机
            channel.exchangeDeclare(exchangeOrderReward, BuiltinExchangeType.TOPIC, true, false, null);
            // 将队列绑定在交换机上,routingKey是key.reward
            channel.queueBind(rewardQueue, exchangeOrderReward, rewardRoutingKey);

            // 绑定监听回调
            channel.basicConsume(rewardQueue, true, deliverCallback, consumerTag -> {});
            while (true) {

            }
        }
    }

    DeliverCallback deliverCallback = (consumerTag, message) -> {
        String messageBody = new String(message.getBody());
        log.info("Reward onMessage---messageBody:{}", messageBody);
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(LOCALHOST);
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

            try (Connection connection = connectionFactory.newConnection();
                 Channel channel = connection.createChannel()) {
                String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                channel.basicPublish(exchangeOrderReward, orderRoutingKey, null, messageToSend.getBytes());
            }
        } catch (JsonProcessingException | TimeoutException e) {
            log.error(e.getMessage(), e);
        }
    };
}
