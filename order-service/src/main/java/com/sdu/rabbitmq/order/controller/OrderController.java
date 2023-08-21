package com.sdu.rabbitmq.order.controller;

import com.sdu.rabbitmq.common.response.ResponseResult;
import com.sdu.rabbitmq.order.entity.dto.PayOrderDTO;
import com.sdu.rabbitmq.order.entity.vo.CreateOrderVO;
import com.sdu.rabbitmq.order.service.OrderService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/order")
@Api("order")
public class OrderController {

    @Resource
    private OrderService orderService;

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public ResponseResult createOrder(@RequestBody @Validated CreateOrderVO orderCreateDTO) {
        return orderService.createOrder(orderCreateDTO);
    }

    @RequestMapping(value = "/pay", method = RequestMethod.POST)
    public ResponseResult payOrder(@RequestBody PayOrderDTO payOrderDTO) {
        return orderService.payOrder(payOrderDTO);
    }
}
