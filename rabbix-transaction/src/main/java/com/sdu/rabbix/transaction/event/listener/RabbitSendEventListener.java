package com.sdu.rabbix.transaction.event.listener;

import com.sdu.rabbix.transaction.event.RabbitSendEvent;
import com.sdu.rabbix.transaction.service.MQProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RabbitSendEventListener {

    @Autowired
    private MQProducer producer;

    @EventListener(classes = RabbitSendEvent.class)
    public void listen(RabbitSendEvent rabbitSendEvent) {
        log.info("message ready to send, id: {}", rabbitSendEvent.getCorrelationData().getId());
        producer.sendMsg(rabbitSendEvent.getExchange(), rabbitSendEvent.getRoutingKey(),
                rabbitSendEvent.getMessage(), rabbitSendEvent.getCorrelationData());
    }
}
