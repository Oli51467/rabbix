package com.sdu.rabbitmq.reward.config;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.sdu.rabbitmq.reward.service.OrderMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static com.sdu.rabbitmq.reward.common.constants.LOCALHOST;

@Slf4j
@Configuration
public class RabbitConfig {

    @Autowired
    private OrderMessageService orderMessageService;

    @Value("${rabbitmq.username}")
    private String rabbitUsername;

    @Value("${rabbitmq.password}")
    private String rabbitPassword;

    @Value("${rabbitmq.host}")
    private String rabbitHost;

    @Value("${rabbitmq.port}")
    private Integer rabbitPort;

    @Value("${rabbitmq.exchange.order-reward}")
    private String orderRewardExchange;

    @Value("${rabbitmq.reward-routing-key}")
    private String rewardRoutingKey;

    @Value("${rabbitmq.reward-queue}")
    private String rewardQueue;

    @Bean
    public Exchange orderRewardExchange() {
        return new TopicExchange(orderRewardExchange);
    }

    @Bean
    public Queue rewardQueue() {
        return new Queue(rewardQueue);
    }

    @Bean
    public Binding orderRewardBinding() {
        return new Binding(rewardQueue, Binding.DestinationType.QUEUE, orderRewardExchange, rewardRoutingKey, null);
    }

    @Bean
    public Channel rabbitChannel() throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(LOCALHOST);
        Connection connection = connectionFactory.newConnection();
        return connection.createChannel();
    }

    @Bean
    public org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory() {
        // 初始化amqp包的连接工厂
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitHost);
        connectionFactory.setPort(rabbitPort);
        connectionFactory.setUsername(rabbitUsername);
        connectionFactory.setPassword(rabbitPassword);
        connectionFactory.createConnection();
        return connectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    @Autowired
    public void startListenMessage() throws IOException {
        orderMessageService.handleMessage();
    }
}
