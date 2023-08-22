package com.sdu.rabbitmq.order.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RefreshScope
@RequestMapping("/redis")
public class RedisTestController {

    @Value("${test.nacos}")
    private String str;

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @Transactional
    public String createOrder() {
        return str;
    }
}
