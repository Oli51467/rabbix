package com.sdu.rabbix.order.service;

import com.sdu.rabbix.common.response.ResponseResult;
import com.sdu.rabbix.order.entity.dto.RegisterReq;

public interface AccountService {

    ResponseResult register(RegisterReq req);
}
