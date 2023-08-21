package com.sdu.rabbitmq.delivery.config;

import com.sdu.rabbitmq.delivery.service.OrderMessageService;
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

    @Value("${rabbitmq.exchange.order-delivery}")
    private String orderDeliveryExchange;

    @Value("${rabbitmq.exchange.dlx}")
    private String dlxExchange;

    @Value("${rabbitmq.delivery-routing-key}")
    private String deliveryRoutingKey;

    @Value("${rabbitmq.delivery-queue}")
    private String deliveryQueue;

    @Bean
    public Exchange orderDeliveryExchange() {
        return ExchangeBuilder.directExchange(orderDeliveryExchange).build();
    }

    @Bean
    public Queue deliveryQueue() {
        return QueueBuilder.durable(deliveryQueue).deadLetterExchange(dlxExchange).deadLetterRoutingKey(deliveryRoutingKey)
                .maxLength(100).build();
    }

    @Bean
    public Binding orderRestaurantBinding(@Qualifier("deliveryQueue") Queue queue,
                                          @Qualifier("orderDeliveryExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(deliveryRoutingKey).noargs();
    }

    @Bean
    public SimpleMessageListenerContainer messageListenerContainer(@Autowired ConnectionFactory connectionFactory,
                                                                   @Autowired OrderMessageService orderMessageService) {
        SimpleMessageListenerContainer messageListenerContainer = new SimpleMessageListenerContainer(connectionFactory);
        // 设置要监听哪几个队列
        messageListenerContainer.setQueueNames(deliveryQueue);
        messageListenerContainer.setExposeListenerChannel(true);
        messageListenerContainer.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        messageListenerContainer.setMessageListener(orderMessageService);
        return messageListenerContainer;
    }
}
