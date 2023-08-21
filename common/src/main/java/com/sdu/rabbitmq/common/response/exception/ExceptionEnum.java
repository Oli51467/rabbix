package com.sdu.rabbitmq.common.response.exception;

import lombok.Getter;

@Getter
public enum ExceptionEnum {

    AUTH_FAILED("token认证失败"),

    USER_NOT_EXIST("用户不存在"),
    USER_EXIST("用户已存在"),
    LOCK_LIMIT("请求太频繁"),
    REPEAT_REQUEST("重复请求"),
    FREQUENCY_LIMIT("请求过于频繁"),
    NOT_AUTH("没有权限"),
    NO_PRODUCT("商品不存在"),
    NO_STOCK("库存不足"),
    ;

    private String msg;

    ExceptionEnum(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "ExceptionEnum{" +
                "msg='" + msg + '\'' +
                '}';
    }
}
