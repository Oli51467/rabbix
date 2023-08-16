package com.sdu.rabbitmq.rdts.service;

import com.sdu.rabbitmq.rdts.annotation.SecureInvoke;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 发送mq工具类
 */
public class MQProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送可靠消息，在事务提交后保证发送成功
     */
    @SecureInvoke
    public void sendSecureMsg(String exchange, String routingKey, Message message, CorrelationData correlationData) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
    }
}
