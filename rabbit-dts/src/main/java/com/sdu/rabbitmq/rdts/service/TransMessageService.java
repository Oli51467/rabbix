package com.sdu.rabbitmq.rdts.service;

import com.sdu.rabbitmq.rdts.domain.entity.TransMessage;

import java.util.List;

public interface TransMessageService {

    /**
     * messageBeforeSend
     * 消息准备好被发送，发送前暂存消息到数据库
     *
     * @param exchange   目标交换机
     * @param routingKey 目标路由key
     * @param body       消息体
     * @return TransMessage 存储到数据库的事务消息
     */
    TransMessage messageBeforeSend(String exchange, String routingKey, String body);

    /**
     * sendMessageSuccess
     * 设置消息发送成功的处理，消息被Mq确认
     *
     * @param id 消息ID
     */
    void sendMessageSuccess(String id);

    /**
     * messageSendReturn
     * 消息无法被路由后返回的处理
     *
     * @param id         事务消息的id
     * @param exchange   目标交换机
     * @param routingKey 目标路由key
     * @param body       消息体
     * @return TransMessage
     */
    TransMessage handleMessageReturn(String id, String exchange, String routingKey, String body);

    /**
     * getReadyMessages
     * 查询准备好发送但还未发送的消息
     *
     * @return List<TransMessage>
     */
    List<TransMessage> getReadyMessages();

    /**
     * resendMessage
     * 消息发送失败后的重试，记录消息发送次数
     *
     * @param id 消息id
     */
    void resendMessage(String id);

    /**
     * messageDead
     * 消息重发多次，放弃
     *
     * @param id 消息id
     */
    void handleMessageDead(String id);

    /**
     * 保存并告警监听到的死信消息
     * @param id 消息id
     * @param exchange 目标交换机
     * @param routingKey 目标路由key
     * @param queue 目标队列
     * @param body 消息体
     */
    void handleMessageDead(String id, String exchange, String routingKey, String queue, String body);

    /**
     * messageBeforeConsume
     * 消息消费前保存
     *
     * @param id         消息id
     * @param exchange   从哪个交换机来
     * @param routingKey 从哪个路由key来
     * @param queue      接收队列
     * @param body       消息体
     * @return TransMessage
     */
    TransMessage messageBeforeConsume(String id, String exchange, String routingKey, String queue, String body);

    /**
     * consumeMessageSuccess
     * 消息消费成功
     *
     * @param id 消息id
     */
    void consumeMessageSuccess(String id);

    /**
     * consumeMessageFailed
     * 消息消费失败
     *
     * @param id 消息id
     */
    void consumeMessageFailed(String id);
}
