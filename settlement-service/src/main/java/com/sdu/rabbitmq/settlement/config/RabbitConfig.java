package com.sdu.rabbitmq.settlement.config;

import com.sdu.rabbitmq.settlement.service.OrderMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.exchange.order-settlement}")
    private String orderSettlementReceiveExchange;

    @Value("${rabbitmq.exchange.settlement-order}")
    private String orderSettlementSendExchange;

    @Value("${rabbitmq.settlement-routing-key}")
    private String settlementRoutingKey;

    @Value("${rabbitmq.settlement-queue}")
    private String settlementQueue;

    @Bean
    public Exchange orderSettlementExchange() {
        return new FanoutExchange(orderSettlementSendExchange);
    }

    @Bean
    public Queue settlementQueue() {
        return new Queue(settlementQueue);
    }

    @Bean
    public Binding orderSettlementBinding() {
        return new Binding(settlementQueue, Binding.DestinationType.QUEUE, orderSettlementReceiveExchange, settlementRoutingKey, null);
    }

    @Bean
    public SimpleMessageListenerContainer messageListenerContainer(@Autowired ConnectionFactory connectionFactory,
                                                                   @Autowired OrderMessageService orderMessageService) {
        SimpleMessageListenerContainer messageListenerContainer = new SimpleMessageListenerContainer(connectionFactory);
        // 设置要监听哪几个队列
        messageListenerContainer.setQueueNames(settlementQueue);
        messageListenerContainer.setExposeListenerChannel(true);
        messageListenerContainer.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        messageListenerContainer.setMessageListener(orderMessageService);
        return messageListenerContainer;
    }
}
