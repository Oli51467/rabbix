package com.sdu.rabbitmq.order.config;

import com.sdu.rabbitmq.order.listener.DelayQueueListener;
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

    @Value("${rabbitmq.exchange.order-reward}")
    public String orderRewardExchange;

    @Value("${rabbitmq.exchange.dlx}")
    private String dlxExchange;

    @Value("${rabbitmq.exchange.ttl}")
    public String ttlExchange;

    @Value("${rabbitmq.order-queue}")
    public String orderQueue;

    @Value("${rabbitmq.order-delay-queue}")
    public String orderDelayQueue;

    @Value("${rabbitmq.ttl-queue}")
    private String ttlQueue;

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
        return QueueBuilder.durable(orderQueue).deadLetterExchange(dlxExchange)
                .deadLetterRoutingKey(orderRoutingKey).maxLength(100).build();
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
    public Exchange ttlExchange() {
        return ExchangeBuilder.topicExchange(ttlExchange).build();
    }

    @Bean
    public Queue ttlQueue() {
        return QueueBuilder.durable(ttlQueue).build();
    }

    @Bean
    public Binding ttlBinding() {
        return BindingBuilder.bind(ttlQueue()).to(ttlExchange()).with("#").noargs();
    }

    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(orderDelayQueue)
                .ttl(60000).deadLetterExchange(ttlExchange).deadLetterRoutingKey(releaseRoutingKey)
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
//    @Bean
//    public Exchange orderSettlementExchange() {
//        return ExchangeBuilder.directExchange(orderSettlementExchange).build();
//    }
//
//    @Bean
//    public Binding orderSettlementBinding(@Qualifier("orderQueue") Queue queue,
//                                          @Qualifier("orderSettlementExchange") Exchange exchange) {
//        return BindingBuilder.bind(queue).to(exchange).with(orderRoutingKey).noargs();
//    }

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

    @Bean
    public SimpleMessageListenerContainer delayQueueListenerContainer(@Autowired ConnectionFactory connectionFactory,
                                                                      @Autowired DelayQueueListener delayQueueListener) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueues(ttlQueue());
        container.setExposeListenerChannel(true);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setMessageListener(delayQueueListener);
        // 设置消费者能处理消息的最大个数
        container.setPrefetchCount(100);
        return container;
    }
}
