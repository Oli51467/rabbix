package com.sdu.rabbitmq.order.config;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.sdu.rabbitmq.order.service.OrderMessageService;
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

    @Value("${rabbitmq.exchange.order-restaurant}")
    private String orderRestaurantExchange;

    @Value("${rabbitmq.exchange.order-delivery}")
    private String orderDeliveryExchange;

    @Value("${rabbitmq.exchange.order-settlement}")
    private String orderSettlementSendExchange;

    @Value("${rabbitmq.exchange.settlement-order}")
    private String orderSettlementReceiveExchange;

    @Value("${rabbitmq.exchange.order-reward}")
    private String orderRewardExchange;

    @Value("${rabbitmq.order-queue}")
    private String orderQueue;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    @Value("${rabbitmq.delivery-routing-key}")
    public String deliveryRoutingKey;

    @Value("${rabbitmq.settlement-routing-key}")
    public String settlementRoutingKey;

    @Value("${rabbitmq.reward-routing-key}")
    public String rewardRoutingKey;

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

    @Bean
    public Channel rabbitChannel() throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitHost);
        Connection connection = connectionFactory.newConnection();
        return connection.createChannel();
    }
}
