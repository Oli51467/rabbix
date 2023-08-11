package com.sdu.rabbitmq.order.utils;

import com.sdu.rabbitmq.order.entity.dto.RequestInfo;

public class UserContextHolder {

    private static final ThreadLocal<RequestInfo> threadLocal = new ThreadLocal<>();

    public static void set(RequestInfo requestInfo) {
        threadLocal.set(requestInfo);
    }

    public static RequestInfo get() {
        return threadLocal.get();
    }

    public static void remove() {
        threadLocal.remove();
    }
}
