package com.sdu.rabbitmq.order.service;

import com.sdu.rabbitmq.order.entity.vo.WebSocketAuthorization;
import io.netty.channel.Channel;

public interface WebSocketService {

    void authorize(Channel channel, WebSocketAuthorization authorizationToken);

    void offline(Channel channel);
}
