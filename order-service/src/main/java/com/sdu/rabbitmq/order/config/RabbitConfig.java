package com.sdu.rabbitmq.order.config;

import com.sdu.rabbitmq.order.service.impl.OrderMessageService;
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

    @Value("${rabbitmq.order-delay-queue}")
    public String orderDelayQueue;

    @Value("${rabbitmq.order-routing-key}")
    public String orderRoutingKey;

    @Value("${rabbitmq.release-routing-key}")
    public String releaseRoutingKey;

    /* -------------------Order to Restaurant-------------------*/
    @Bean
    public Exchange orderRestaurantExchange() {
        return ExchangeBuilder.directExchange(orderRestaurantExchange).build();
    }

    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(orderQueue).build();
    }

    @Bean
    public Binding orderRestaurantBinding(@Qualifier("orderQueue") Queue queue,
                                          @Qualifier("orderRestaurantExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(orderRoutingKey).noargs();
    }

    /*
    * 声明ttl队列 同时绑定死信交换机
    * 消息在ttl队列里并不会被消费，等待超时后被自动转发到死信交换机，最终到达死信队列做订单超时的处理
    */
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(orderDelayQueue)
                .ttl(60000).deadLetterExchange("exchange.dlx").deadLetterRoutingKey(releaseRoutingKey)
                .maxLength(100).build();
    }

    @Bean
    public Binding orderReleaseBinding(@Qualifier("orderDelayQueue") Queue queue,
                                       @Qualifier("orderRestaurantExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(releaseRoutingKey).noargs();
    }

    /* -------------------Order to Delivery-------------------*/
    @Bean
    public Exchange orderDeliveryExchange() {
        return ExchangeBuilder.directExchange(orderDeliveryExchange).build();
    }

    @Bean
    public Binding orderDeliveryBinding(@Qualifier("orderQueue") Queue queue,
                                        @Qualifier("orderDeliveryExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(orderRoutingKey).noargs();
    }

    /* -------------------Order to Settlement-------------------*/
    @Bean
    public Exchange orderSettlementExchange() {
        return ExchangeBuilder.fanoutExchange(orderSettlementSendExchange).build();
    }

    @Bean
    public Exchange settlementOrderExchange() {
        return ExchangeBuilder.fanoutExchange(orderSettlementReceiveExchange).build();
    }

    @Bean
    public Binding orderSettlementBinding(@Qualifier("orderQueue") Queue queue) {
        return BindingBuilder.bind(queue).to(settlementOrderExchange()).with(orderRoutingKey).noargs();
    }

    /* -------------------Order to Reward-------------------*/
    @Bean
    public Exchange orderRewardExchange() {
        return ExchangeBuilder.topicExchange(orderRewardExchange).build();
    }

    @Bean
    public Binding orderRewardBinding(@Qualifier("orderQueue") Queue queue,
                                      @Qualifier("orderRewardExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(orderRoutingKey).noargs();
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
