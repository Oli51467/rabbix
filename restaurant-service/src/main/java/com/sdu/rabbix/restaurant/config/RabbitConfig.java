package com.sdu.rabbix.restaurant.config;

import com.sdu.rabbix.restaurant.service.OrderMessageService;
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

    @Value("${rabbitmq.exchange.order-restaurant}")
    private String orderRestaurantExchange;

    @Value("${rabbitmq.exchange.dlx}")
    private String dlxExchange;

    @Value("${rabbitmq.restaurant-routing-key}")
    private String restaurantRoutingKey;

    @Value("${rabbitmq.restaurant-queue}")
    private String restaurantQueue;

    @Bean
    public Exchange orderRestaurantExchange() {
        return ExchangeBuilder.directExchange(orderRestaurantExchange).build();
    }

    @Bean
    public Queue restaurantQueue() {
        return QueueBuilder.durable(restaurantQueue).deadLetterExchange(dlxExchange).deadLetterRoutingKey(restaurantRoutingKey)
                .maxLength(100).build();
    }

    @Bean
    public Binding orderRestaurantBinding(@Qualifier("restaurantQueue") Queue queue,
                                          @Qualifier("orderRestaurantExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(restaurantRoutingKey).noargs();
    }

    @Bean
    public SimpleMessageListenerContainer messageListenerContainer(@Autowired ConnectionFactory connectionFactory,
                                                                   @Autowired OrderMessageService orderMessageService) {
        SimpleMessageListenerContainer messageListenerContainer = new SimpleMessageListenerContainer(connectionFactory);
        // 设置要监听哪几个队列
        messageListenerContainer.setQueueNames(restaurantQueue);
        messageListenerContainer.setExposeListenerChannel(true);
        messageListenerContainer.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        messageListenerContainer.setMessageListener(orderMessageService);
        return messageListenerContainer;
    }
}
