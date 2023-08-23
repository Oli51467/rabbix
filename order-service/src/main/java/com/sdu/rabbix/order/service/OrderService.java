package com.sdu.rabbix.order.service;

import com.sdu.rabbix.common.response.ResponseResult;
import com.sdu.rabbix.order.entity.vo.CreateOrderVO;

public interface OrderService {

    ResponseResult createOrder(CreateOrderVO createOrderVO);
}
