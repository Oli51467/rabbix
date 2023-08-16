package com.sdu.rabbitmq.rdts.config;

import com.sdu.rabbitmq.rdts.listener.AbstractDlxListener;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("rdts.dlxEnabled")
public class DlxConfig {

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange("exchange.dlx");
    }

    @Bean
    public Queue dlxQueue() {
        return new Queue("queue.dlx", true, false, false);
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with("#");
    }

    @Bean
    public SimpleMessageListenerContainer deadLetterListenerContainer(ConnectionFactory connectionFactory, AbstractDlxListener abstractDlxListener) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueues(dlxQueue());
        container.setExposeListenerChannel(true);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setMessageListener(abstractDlxListener);
        // 设置消费者能处理消息的最大个数
        container.setPrefetchCount(100);
        return container;
    }
}
