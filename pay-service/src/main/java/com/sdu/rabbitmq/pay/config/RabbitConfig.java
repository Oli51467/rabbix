package com.sdu.rabbitmq.pay.config;

import com.sdu.rabbitmq.pay.service.OrderMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.exchange.order-settlement}")
    private String orderSettlementExchange;

    @Value("${rabbitmq.exchange.dlx}")
    private String dlxExchange;

    @Value("${rabbitmq.settlement-routing-key}")
    private String settlementRoutingKey;

    @Value("${rabbitmq.settlement-queue}")
    private String settlementQueue;

    @Bean
    public Exchange orderSettlementExchange() {
        return ExchangeBuilder.directExchange(orderSettlementExchange).build();
    }

    @Bean
    public Queue settlementQueue() {
        return QueueBuilder.durable(settlementQueue).deadLetterExchange(dlxExchange)
                .deadLetterRoutingKey(settlementRoutingKey).maxLength(100).build();
    }

    @Bean
    public Binding orderSettlementBinding(@Qualifier("settlementQueue") Queue queue,
                                          @Qualifier("orderSettlementExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(settlementRoutingKey).noargs();
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
