package com.sdu.rabbitmq.reward.config;

import com.sdu.rabbitmq.reward.service.OrderMessageService;
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

    @Value("${rabbitmq.exchange.order-reward}")
    private String orderRewardExchange;

    @Value("${rabbitmq.exchange.dlx}")
    private String dlxExchange;

    @Value("${rabbitmq.reward-routing-key}")
    private String rewardRoutingKey;

    @Value("${rabbitmq.reward-queue}")
    private String rewardQueue;

    @Bean
    public Exchange orderRewardExchange() {
        return ExchangeBuilder.topicExchange(orderRewardExchange).build();
    }

    @Bean
    public Queue rewardQueue() {
        return QueueBuilder.durable(rewardQueue).deadLetterExchange(dlxExchange).deadLetterRoutingKey(rewardRoutingKey)
                .maxLength(100).build();
    }

    @Bean
    public Binding orderRewardBinding(@Qualifier("rewardQueue") Queue queue,
                                      @Qualifier("orderRewardExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(rewardRoutingKey).noargs();
    }

    @Bean
    public SimpleMessageListenerContainer messageListenerContainer(@Autowired ConnectionFactory connectionFactory,
                                                                   @Autowired OrderMessageService orderMessageService) {
        SimpleMessageListenerContainer messageListenerContainer = new SimpleMessageListenerContainer(connectionFactory);
        // 设置要监听哪几个队列
        messageListenerContainer.setQueueNames(rewardQueue);
        messageListenerContainer.setExposeListenerChannel(true);
        messageListenerContainer.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        messageListenerContainer.setMessageListener(orderMessageService);
        return messageListenerContainer;
    }
}
