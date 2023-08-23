package com.sdu.rabbix.order.controller;

import com.sdu.rabbix.common.response.ResponseResult;
import com.sdu.rabbix.order.entity.vo.CreateOrderVO;
import com.sdu.rabbix.order.service.OrderService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
}
