package com.sdu.rabbitmq.order.controller;

import com.sdu.rabbitmq.order.entity.vo.CreateOrderVO;
import com.sdu.rabbitmq.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Resource
    OrderService orderService;

    @PostMapping("/create")
    public void createOrder(@RequestBody CreateOrderVO orderCreateDTO) {
        log.info("createOrder:orderCreateDTO:{}", orderCreateDTO);
        orderService.createOrder(orderCreateDTO);
    }
}
