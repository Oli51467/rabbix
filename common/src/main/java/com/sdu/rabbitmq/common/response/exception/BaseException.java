package com.sdu.rabbitmq.common.response.exception;

import com.sdu.rabbitmq.common.response.ResponseCode;

/**
 * 业务异常类，继承运行时异常，确保事务正常回滚
 */
public class BaseException extends RuntimeException{

    private ResponseCode code;

    public BaseException(ResponseCode code) {
        this.code = code;
    }

    public BaseException(Throwable cause, ResponseCode code) {
        super(cause);
        this.code = code;
    }

    public ResponseCode getCode() {
        return code;
    }

    public void setCode(ResponseCode code) {
        this.code = code;
    }
}
