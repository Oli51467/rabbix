package com.sdu.rabbitmq.rdts.scheduler;

import com.sdu.rabbitmq.rdts.domain.TransMessage;
import com.sdu.rabbitmq.rdts.service.TransMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@EnableScheduling
@Configuration
@Component
@Slf4j
public class ResendMessageScheduler {

    @Value("${rdts.resend-time}")
    private Integer resendTimes;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TransMessageService transMessageService;

    @Scheduled(fixedDelayString = "${rdts.resend-frequency}")
    public void resendMessage() {
        log.info("resendMessage scheduler invoked!");
        List<TransMessage> readyMessages = transMessageService.getReadyMessages();
        log.info("ready messages count: {}", readyMessages.size());

        for (TransMessage readyMessage : readyMessages) {
            log.info("ready message: {}", readyMessage);
            if (readyMessage.getSequence() > resendTimes) {
                log.error("message {} resend too many time", readyMessage.getId());
                transMessageService.handleMessageDead(readyMessage.getId());
            } else {
                // rabbitTemplate 发送给MQ
                MessageProperties messageProperties = new MessageProperties();
                messageProperties.setContentType("application/json");
                Message message = new Message(readyMessage.getPayload().getBytes(), messageProperties);
                message.getMessageProperties().setMessageId(readyMessage.getId());

                rabbitTemplate.convertAndSend(readyMessage.getExchange(), readyMessage.getRoutingKey(), message, new CorrelationData(readyMessage.getId()));

                log.info("message sent from transmitter, id: {}", readyMessage.getId());
                // 重试次数+1
                transMessageService.resendMessage(readyMessage.getId());
            }
        }
    }
}