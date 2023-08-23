package com.sdu.rabbix.order.controller;

import com.sdu.rabbix.common.response.ResponseResult;
import com.sdu.rabbix.order.entity.dto.PayOrderDTO;
import com.sdu.rabbix.order.service.PayService;
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
