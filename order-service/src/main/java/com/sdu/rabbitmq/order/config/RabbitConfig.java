package com.sdu.rabbitmq.order.config;

import com.sdu.rabbitmq.order.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.order.service.OrderMessageService;
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

    @Value("${rabbitmq.order-routing-key}")
    public String orderRoutingKey;

    @Resource
    private OrderMessageService orderMessageService;

    private static RabbitTemplate rabbitTemplate;

    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        RabbitConfig.rabbitTemplate = rabbitTemplate;
    }

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
    public RabbitTemplate rabbitTemplate(@Autowired ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // 设置托管状态
        rabbitTemplate.setMandatory(true);
        // 设置消息返回的回调
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.info("Return Callback---message: {}, replyCode: {}, replyText: {}, exchange: {}, routingKey: {}",
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
        messageListenerContainer.setQueueNames(orderQueue);
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
