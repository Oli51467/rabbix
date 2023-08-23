package com.sdu.rabbix.order.service;

import com.sdu.rabbix.order.entity.vo.WebSocketAuthorization;
import io.netty.channel.Channel;

public interface WebSocketService {

    void authorize(Channel channel, WebSocketAuthorization authorizationToken);

    void offline(Channel channel);
}
