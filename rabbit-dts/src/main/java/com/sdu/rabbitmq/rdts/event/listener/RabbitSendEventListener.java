package com.sdu.rabbitmq.rdts.event.listener;

import com.sdu.rabbitmq.rdts.event.RabbitSendEvent;
import com.sdu.rabbitmq.rdts.service.MQProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@EnableTransactionManagement
public class RabbitSendEventListener {

    @Autowired
    private MQProducer producer;

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, classes = RabbitSendEvent.class, fallbackExecution = true)
    public void listen(RabbitSendEvent rabbitSendEvent) {
        log.info("message ready to send, id: {}", rabbitSendEvent.getCorrelationData().getId());
        producer.sendSecureMsg(rabbitSendEvent.getExchange(), rabbitSendEvent.getRoutingKey(),
                rabbitSendEvent.getMessage(), rabbitSendEvent.getCorrelationData());
    }
}
