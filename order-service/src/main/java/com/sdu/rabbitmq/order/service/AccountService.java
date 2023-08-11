package com.sdu.rabbitmq.order.service;

import com.sdu.rabbitmq.common.response.ResponseResult;
import com.sdu.rabbitmq.order.entity.dto.RegisterReq;

public interface AccountService {

    ResponseResult register(RegisterReq req);
}
