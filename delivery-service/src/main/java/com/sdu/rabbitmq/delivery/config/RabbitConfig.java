package com.sdu.rabbitmq.delivery.config;

import com.sdu.rabbitmq.delivery.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.delivery.service.OrderMessageService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.ClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Slf4j
@Configuration
public class RabbitConfig {

    @Resource
    OrderMessageService orderMessageService;

    @Value("${rabbitmq.exchange.order-delivery}")
    private String orderDeliveryExchange;

    @Value("${rabbitmq.delivery-routing-key}")
    private String deliveryRoutingKey;

    @Value("${rabbitmq.delivery-queue}")
    private String deliveryQueue;

    private static RabbitTemplate rabbitTemplate;

    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        RabbitConfig.rabbitTemplate = rabbitTemplate;
    }

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
    public RabbitTemplate rabbitTemplate(@Autowired ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // 设置托管状态
        rabbitTemplate.setMandatory(true);
        // 设置消息返回的回调
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.info("Delivery return callback---message: {}, replyCode: {}, replyText: {}, exchange: {}, routingKey: {}",
                    message, replyCode, replyText, exchange, routingKey);
        });
        // 设置确认消息从RabbitMQ发出的回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("RabbitMQ confirm send success");
            } else {
                log.error("RabbitMQ confirm send failed");
                // TODO:根据CorrelationData的信息将订单状态设置为失败
            }
            log.info("Confirm Callback---correlationData: {}, ack: {}, cause: {}", correlationData, ack, cause);
        });
        return rabbitTemplate;
    }

    @Bean
    public SimpleMessageListenerContainer messageListenerContainer(@Autowired ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer messageListenerContainer = new SimpleMessageListenerContainer(connectionFactory);
        // 设置要监听哪几个队列
        messageListenerContainer.setQueueNames(deliveryQueue);
        // 使用适配器模式优雅调用service服务设置收到消息的回调 设置代理为服务类
        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(orderMessageService);
        // 将[]byte格式转化为DTO 需要使用MessageConverter的实现类
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setClassMapper(new ClassMapper() {
            @Override
            public void fromClass(@NotNull Class<?> aClass, @NotNull MessageProperties messageProperties) {}

            @NotNull
            @Override
            public Class<?> toClass(@NotNull MessageProperties messageProperties) {
                return OrderMessageDTO.class;
            }
        });

        // 设置类型转换器
        messageListenerAdapter.setMessageConverter(converter);
        messageListenerContainer.setMessageListener(messageListenerAdapter);
        return messageListenerContainer;
    }

    public static void sendToRabbit(String exchange, String routingKey, String messageToSend) {
        MessageProperties messageProperties = new MessageProperties();

        Message message = new Message(messageToSend.getBytes(), messageProperties);
        rabbitTemplate.send(exchange, routingKey, message);
    }
}
