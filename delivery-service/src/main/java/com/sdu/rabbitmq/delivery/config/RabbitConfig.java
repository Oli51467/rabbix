package com.sdu.rabbitmq.delivery.config;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.sdu.rabbitmq.delivery.service.OrderMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static com.sdu.rabbitmq.delivery.common.constants.LOCALHOST;

@Slf4j
@Configuration
public class RabbitConfig {

    @Autowired
    OrderMessageService orderMessageService;

    @Value("${rabbitmq.username}")
    private String rabbitUsername;

    @Value("${rabbitmq.password}")
    private String rabbitPassword;

    @Value("${rabbitmq.host}")
    private String rabbitHost;

    @Value("${rabbitmq.port}")
    private Integer rabbitPort;

    @Value("${rabbitmq.exchange.order-delivery}")
    private String orderDeliveryExchange;

    @Value("${rabbitmq.delivery-routing-key}")
    private String deliveryRoutingKey;

    @Value("${rabbitmq.delivery-queue}")
    private String deliveryQueue;

    @Bean
    public Exchange orderDeliveryExchange() {
        return new DirectExchange(orderDeliveryExchange);
    }

    @Bean
    public Queue deliveryQueue() {
        return new Queue(deliveryQueue);
    }

    @Bean
    public Binding orderRestaurantBinding() {
        return new Binding(deliveryQueue, Binding.DestinationType.QUEUE, orderDeliveryExchange, deliveryRoutingKey, null);
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
