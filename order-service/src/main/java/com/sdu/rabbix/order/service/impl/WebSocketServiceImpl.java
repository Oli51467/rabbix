package com.sdu.rabbix.order.service.impl;

import com.sdu.rabbix.order.entity.vo.WebSocketAuthorization;
import com.sdu.rabbix.order.service.WebSocketService;
import io.netty.channel.Channel;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketServiceImpl implements WebSocketService {

    private static final ConcurrentHashMap<Long, Channel> ONLINE_USERS = new ConcurrentHashMap<>(8);
    private static final ConcurrentHashMap<Channel, Long> CHANNEL_MAP = new ConcurrentHashMap<>(8);

    @Override
    public void authorize(Channel channel, WebSocketAuthorization authorizationToken) {

    }

    @Override
    public void offline(Channel channel) {
        Long uid = CHANNEL_MAP.get(channel);
        CHANNEL_MAP.remove(channel);
        ONLINE_USERS.remove(uid);
    }
}
