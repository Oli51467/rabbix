package com.sdu.rabbix.transaction.transmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbix.transaction.domain.entity.TransMessage;
import com.sdu.rabbix.transaction.factory.EventFactory;
import com.sdu.rabbix.transaction.service.TransMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/*
业务代码与RabbitTemplate的中间层
 */
@Component
@Slf4j
public class TransMessageTransmitter {

    @Value("${rabbix.content-type}")
    private String contentType;

    @Value("${rabbix.transaction-method}")
    private String transactionMethod;

    @Resource
    private TransMessageService transMessageService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private EventFactory eventFactory;

    public void send(String exchange, String routingKey, Object payload) throws JsonProcessingException {
        log.info("send(): exchange: {}, routingKey: {}, payload: {}", exchange, routingKey, payload);

        // 将要发送的各种类型的数据结构序列化
        ObjectMapper objectMapper = new ObjectMapper();
        String payloadStr = objectMapper.writeValueAsString(payload);
        // 调用发送前的服务
        TransMessage transMessage = transMessageService.messageBeforeSend(exchange, routingKey, payloadStr);
        // rabbitTemplate 发送给MQ
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(contentType);
        Message message = new Message(payloadStr.getBytes(), messageProperties);
        message.getMessageProperties().setMessageId(transMessage.getId());

        applicationEventPublisher.publishEvent(eventFactory.createEvent(transactionMethod,
                exchange, routingKey, message, new CorrelationData(transMessage.getId())));
    }
}
