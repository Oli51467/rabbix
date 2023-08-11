package com.sdu.rabbitmq.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public enum WebSocketEventTypeEnum {

    LOGIN(1, "请求二维码"),
    HEARTBEAT(2, "心跳"),
    AUTHORIZE(3, "登录认证"),
    ;

    private final Integer type;
    private final String desc;

    private static Map<Integer, WebSocketEventTypeEnum> cache;

    static {
        cache = Arrays.stream(WebSocketEventTypeEnum.values()).collect(Collectors.toMap(WebSocketEventTypeEnum::getType, Function.identity()));
    }

    public static WebSocketEventTypeEnum of(Integer type) {
        return cache.get(type);
    }
}
