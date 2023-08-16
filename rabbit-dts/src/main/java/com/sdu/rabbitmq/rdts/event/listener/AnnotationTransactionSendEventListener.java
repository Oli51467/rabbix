package com.sdu.rabbitmq.rdts.event.listener;

import com.sdu.rabbitmq.rdts.event.AnnotationTransactionSendEvent;
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
public class AnnotationTransactionSendEventListener {

    @Autowired
    private MQProducer producer;

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, classes = AnnotationTransactionSendEvent.class, fallbackExecution = true)
    public void listen(AnnotationTransactionSendEvent annotationTransactionSendEvent) {
        log.info("message ready to send, id: {}", annotationTransactionSendEvent.getCorrelationData().getId());
        producer.sendSecureMsg(annotationTransactionSendEvent.getExchange(), annotationTransactionSendEvent.getRoutingKey(),
                annotationTransactionSendEvent.getMessage(), annotationTransactionSendEvent.getCorrelationData());
    }
}
