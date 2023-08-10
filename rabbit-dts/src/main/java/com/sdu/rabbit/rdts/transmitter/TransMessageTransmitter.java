package com.sdu.rabbit.rdts.transmitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbit.rdts.domain.TransMessage;
import com.sdu.rabbit.rdts.service.TransMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/*
业务代码与RabbitTemplate的中间层，直接操作RabbitTemplate
 */
@Component
@Slf4j
public class TransMessageTransmitter {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TransMessageService transMessageService;

    public void send(String exchange, String routingKey, Object payload) {
        log.info("send(): exchange: {}, routingKey: {}, payload: {}", exchange, routingKey, payload);

        try {
            // 将要发送的各种类型的数据结构序列化
            ObjectMapper objectMapper = new ObjectMapper();
            String payloadStr = objectMapper.writeValueAsString(payload);
            // 调用发送前的服务
            TransMessage transMessage = transMessageService.messageBeforeSend(exchange, routingKey, payloadStr);
            // rabbitTemplate 发送给MQ
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType("application/json");
            Message message = new Message(payloadStr.getBytes(), messageProperties);
            message.getMessageProperties().setMessageId(transMessage.getId());

            rabbitTemplate.convertAndSend(exchange, routingKey, message, new CorrelationData(transMessage.getId()));

            log.info("message sent from transmitter, id: {}", transMessage.getId());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
