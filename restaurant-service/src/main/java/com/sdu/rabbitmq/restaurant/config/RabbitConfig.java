package com.sdu.rabbitmq.restaurant.config;

import com.sdu.rabbitmq.restaurant.service.OrderMessageService;
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

    @Value("${rabbitmq.exchange.order-restaurant}")
    private String orderRestaurantExchange;

    @Value("${rabbitmq.restaurant-routing-key}")
    private String restaurantRoutingKey;

    @Value("${rabbitmq.restaurant-queue}")
    private String restaurantQueue;

    @Bean
    public Exchange orderRestaurantExchange() {
        return new DirectExchange(orderRestaurantExchange);
    }

    @Bean
    public Queue restaurantQueue() {
        return new Queue(restaurantQueue);
    }

    @Bean
    public Binding orderRestaurantBinding() {
        return new Binding(restaurantQueue, Binding.DestinationType.QUEUE, orderRestaurantExchange, restaurantRoutingKey, null);
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
