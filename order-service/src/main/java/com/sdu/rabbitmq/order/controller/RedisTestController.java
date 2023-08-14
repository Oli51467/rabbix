package com.sdu.rabbitmq.order.controller;

import com.sdu.rabbitmq.common.utils.RedisUtil;
import com.sdu.rabbitmq.order.entity.vo.CreateOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/redis")
public class RedisTestController {

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public void createOrder() {
        RedisUtil.set("k", "vvv");
        log.info(RedisUtil.get("k"));
    }
}
