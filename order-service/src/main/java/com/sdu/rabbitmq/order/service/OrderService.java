package com.sdu.rabbitmq.order.service;

import com.sdu.rabbitmq.order.entity.vo.CreateOrderVO;

public interface OrderService {

    void createOrder(CreateOrderVO createOrderVO);
}
