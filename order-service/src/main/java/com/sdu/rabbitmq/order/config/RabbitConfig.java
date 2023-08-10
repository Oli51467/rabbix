package com.sdu.rabbitmq.order.config;

import com.sdu.rabbitmq.order.service.OrderMessageService;
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
    public String orderRestaurantExchange;

    @Value("${rabbitmq.exchange.order-delivery}")
    public String orderDeliveryExchange;

    @Value("${rabbitmq.exchange.order-settlement}")
    public String orderSettlementSendExchange;

    @Value("${rabbitmq.exchange.settlement-order}")
    public String orderSettlementReceiveExchange;

    @Value("${rabbitmq.exchange.order-reward}")
    public String orderRewardExchange;

    @Value("${rabbitmq.order-queue}")
    public String orderQueue;

    @Value("${rabbitmq.order-routing-key}")
    public String orderRoutingKey;

    /* -------------------Order to Restaurant-------------------*/
    @Bean
    public Exchange orderRestaurantExchange() {
        return new DirectExchange(orderRestaurantExchange);
    }

    @Bean
    public Queue orderQueue() {
        return new Queue(orderQueue);
    }

    @Bean
    public Binding orderRestaurantBinding() {
        return new Binding(orderQueue, Binding.DestinationType.QUEUE, orderRestaurantExchange, orderRoutingKey, null);
    }

    /* -------------------Order to Delivery-------------------*/
    @Bean
    public Exchange orderDeliveryExchange() {
        return new DirectExchange(orderDeliveryExchange);
    }

    @Bean
    public Binding orderDeliveryBinding() {
        return new Binding(orderQueue, Binding.DestinationType.QUEUE, orderDeliveryExchange, orderRoutingKey, null);
    }

    /* -------------------Order to Settlement-------------------*/
    @Bean
    public Exchange orderSettlementExchange() {
        return new FanoutExchange(orderSettlementSendExchange);
    }

    @Bean
    public Exchange settlementOrderExchange() {
        return new FanoutExchange(orderSettlementReceiveExchange);
    }

    @Bean
    public Binding orderSettlementBinding() {
        return new Binding(orderQueue, Binding.DestinationType.QUEUE, orderSettlementReceiveExchange, orderRoutingKey, null);
    }

    /* -------------------Order to Reward-------------------*/
    @Bean
    public Exchange orderRewardExchange() {
        return new TopicExchange(orderRewardExchange);
    }

    @Bean
    public Binding orderRewardBinding() {
        return new Binding(orderQueue, Binding.DestinationType.QUEUE, orderRewardExchange, orderRoutingKey, null);
    }

    @Bean
    public SimpleMessageListenerContainer messageListenerContainer(@Autowired ConnectionFactory connectionFactory,
                                                                   @Autowired OrderMessageService orderMessageService) {
        SimpleMessageListenerContainer messageListenerContainer = new SimpleMessageListenerContainer(connectionFactory);
        // 设置要监听哪几个队列
        messageListenerContainer.setQueueNames(orderQueue);
        messageListenerContainer.setExposeListenerChannel(true);
        messageListenerContainer.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        messageListenerContainer.setMessageListener(orderMessageService);
        return messageListenerContainer;
    }
}
