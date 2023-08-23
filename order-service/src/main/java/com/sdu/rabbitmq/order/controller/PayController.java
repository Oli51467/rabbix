package com.sdu.rabbitmq.order.controller;

import com.sdu.rabbitmq.common.response.ResponseResult;
import com.sdu.rabbitmq.order.entity.dto.PayOrderDTO;
import com.sdu.rabbitmq.order.service.PayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/alipay")
public class PayController {

    @Autowired
    private PayService payService;

    @RequestMapping(value = "/pay", method = RequestMethod.POST)
    public ResponseResult payOrder(@RequestBody PayOrderDTO payOrderDTO) {
        return payService.payOrder(payOrderDTO.getOrderId());
    }

    @RequestMapping(value = "/checkRsa", method = RequestMethod.POST)
    public ResponseResult payOrder(@RequestParam Map<String, String> params) {
        return payService.checkRsaV1(params);
    }

    @RequestMapping(value = "/refund", method = RequestMethod.POST)
    public ResponseResult refund(@RequestParam Long orderId) {
        return payService.refund(orderId);
    }
}
