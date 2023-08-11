package com.sdu.rabbitmq.common.response.exception;

import lombok.Getter;

@Getter
public enum ExceptionEnum {

    AUTH_FAILED("token认证失败"),

    USER_NOT_EXIST("用户不存在"),
    USER_EXIST("用户已存在"),
    NOT_AUTH("没有权限"),
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
