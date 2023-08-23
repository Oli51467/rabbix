package com.sdu.rabbix.transaction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbix.transaction.service.TransMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@Slf4j
public class RabbitDistributedTransactionConfig {

    @Resource
    private TransMessageService transMessageService;

    @Value("${rabbix.host}")
    private String host;

    @Value("${rabbix.port}")
    private Integer port;

    @Value("${rabbix.username}")
    private String username;

    @Value("${rabbix.password}")
    private String password;

    @Value("${rabbix.vhost}")
    private String vhost;

    @Value("${rabbix.concurrent-consumers}")
    private Integer concurrentConsumers;

    @Value("${rabbix.max-concurrent-consumers}")
    private Integer maxConcurrentConsumers;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(vhost);
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(true);
        connectionFactory.createConnection();
        return connectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public RabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(concurrentConsumers);
        factory.setMaxConcurrentConsumers(maxConcurrentConsumers);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }

    @Bean
    public RabbitTemplate customRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            log.info("correlationData:{}, ack:{}, cause:{}", correlationData, ack, cause);
            if (ack && null != correlationData) {
                // 通过correlationData找到确认的是哪条消息
                String messageId = correlationData.getId();
                log.info("消息已经正确投递到交换机, id:{}", messageId);
                transMessageService.sendMessageSuccess(messageId);
            } else {
                // 如果投递失败 定时任务会讲消息捞起来
                log.error("消息投递至交换机失败, correlationData:{}", correlationData);
            }
        });
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.error("消息无法路由！message:{}, replyCode:{} replyText:{} exchange:{} routingKey:{}", message, replyCode, replyText, exchange, routingKey);
            transMessageService.handleMessageReturn(message.getMessageProperties().getMessageId(), exchange, routingKey, new String(message.getBody()));
        });
        return rabbitTemplate;
    }
}
