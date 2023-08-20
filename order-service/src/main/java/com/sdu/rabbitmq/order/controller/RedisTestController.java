package com.sdu.rabbitmq.order.controller;

import com.sdu.rabbitmq.order.repository.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/redis")
public class RedisTestController {

    @Resource
    private ProductMapper productMapper;

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @Transactional
    public void createOrder() {
        int lockStatus = productMapper.lockStock(2L, 1);
        System.out.println(lockStatus);

    }
}
