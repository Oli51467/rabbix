package com.sdu.rabbitmq.order.controller;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sdu.rabbitmq.common.utils.RedisUtil;
import com.sdu.rabbitmq.order.entity.po.Product;
import com.sdu.rabbitmq.order.entity.vo.CreateOrderVO;
import com.sdu.rabbitmq.order.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/redis")
public class RedisTestController {

    @Resource
    private ProductMapper productMapper;

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @Transactional
    public void createOrder() {
        int lockStatus = productMapper.lockStock(2L);
        System.out.println(lockStatus);

    }
}
