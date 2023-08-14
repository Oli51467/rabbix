package com.sdu.rabbitmq.common.response.exception;

import lombok.Data;

/**
 * 自定义限流异常
 */
@Data
public class FrequencyControlException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    protected Integer errorCode;

    /**
     * 错误信息
     */
    protected String errorMsg;

    public FrequencyControlException(ExceptionEnum error) {
        super(error.getMsg());
        this.errorMsg = error.getMsg();
    }
}
