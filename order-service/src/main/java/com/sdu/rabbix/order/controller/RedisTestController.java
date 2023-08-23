package com.sdu.rabbix.order.controller;

import com.sdu.rabbix.order.config.AlipayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/redis")
public class RedisTestController {

    @Autowired
    private AlipayProperties alipayProperties;

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public String createOrder() {
        return alipayProperties.getAlipayPublicKey();
    }
}
