package com.sdu.rabbit.rdts.listener;

import com.rabbitmq.client.Channel;
import com.sdu.rabbit.rdts.service.TransMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 系统死信监听的类，可配置是否监听死信队列
 */
@Component
@Slf4j
@ConditionalOnProperty("rdts.dlxEnabled")
public class DlxListener implements ChannelAwareMessageListener {

    @Autowired
    private TransMessageService transMessageService;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        String messageBody = new String(message.getBody());
        log.error("dead letter! message: {}", message);

        MessageProperties properties = message.getMessageProperties();
        transMessageService.handleMessageDead(
                properties.getMessageId(), properties.getReceivedExchange(), properties.getReceivedRoutingKey(),
                properties.getConsumerQueue(), messageBody);

        channel.basicAck(properties.getDeliveryTag(), false);
    }
}
