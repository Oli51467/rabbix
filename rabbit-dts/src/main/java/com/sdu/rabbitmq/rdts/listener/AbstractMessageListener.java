package com.sdu.rabbitmq.rdts.listener;

import com.rabbitmq.client.Channel;
import com.sdu.rabbitmq.rdts.domain.entity.TransMessage;
import com.sdu.rabbitmq.rdts.service.TransMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public abstract class AbstractMessageListener implements ChannelAwareMessageListener {

    @Autowired
    private TransMessageService transMessageService;

    @Value("${rdts.resend-time}")
    private Integer resendTimes;

    public abstract void receiveMessage(Message message) throws IOException;

    @Override
    public void onMessage(Message message, Channel channel) throws InterruptedException, IOException {
        MessageProperties properties = message.getMessageProperties();
        long deliveryTag = properties.getDeliveryTag();

        TransMessage transMessage = transMessageService.messageBeforeConsume(
                properties.getMessageId(), properties.getReceivedExchange(),
                properties.getReceivedRoutingKey(), properties.getConsumerQueue(), new String(message.getBody()));
        log.info("消费端收到消息, id: {}, 消费次数: {}", transMessage.getId(), transMessage.getSequence());

        try {
            // 消费端需要实现该类的抽象方法，然后调用具体业务的接收处理方法
            receiveMessage(message);
            channel.basicAck(deliveryTag, false);
            transMessageService.consumeMessageSuccess(transMessage.getId());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (transMessage.getSequence() > resendTimes) {
                channel.basicReject(deliveryTag, false);
                transMessageService.consumeMessageFailed(transMessage.getId());
            } else {
                // 等待一段时间，等待时间由重试次数决定
                Thread.sleep((long) (Math.pow(2, transMessage.getSequence()) * 1000));
                // 拒收消息 并设置为重回队列
                channel.basicNack(deliveryTag, false, true);
            }
        }
    }
}
