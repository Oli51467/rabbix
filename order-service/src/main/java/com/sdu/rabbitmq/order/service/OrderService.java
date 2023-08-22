package com.sdu.rabbitmq.order.service;

import com.sdu.rabbitmq.common.response.ResponseResult;
import com.sdu.rabbitmq.order.entity.vo.CreateOrderVO;

public interface OrderService {

    ResponseResult createOrder(CreateOrderVO createOrderVO);

    ResponseResult payOrder(Long orderId);
}
