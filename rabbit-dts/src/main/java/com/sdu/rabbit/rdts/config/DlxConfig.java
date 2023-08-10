package com.sdu.rabbit.rdts.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
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
}
