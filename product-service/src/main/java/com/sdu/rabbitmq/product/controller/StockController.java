package com.sdu.rabbitmq.product.controller;

import com.sdu.rabbitmq.product.repository.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/stock")
@Slf4j
public class StockController {

    @Resource
    private ProductMapper productMapper;

    @RequestMapping(value = "/lock", method = RequestMethod.POST)
    @Transactional
    public Integer lockStock(@RequestParam("productId") Long productId, @RequestParam("count") Integer count) {
        log.info("lock stock called by feign!");
        return productMapper.lockStock(productId, count);
    }

    @RequestMapping(value = "/unlock", method = RequestMethod.POST)
    @Transactional
    public Integer unlockStock(@RequestParam("productId") Long productId, @RequestParam("count") Integer count) {
        log.info("unlock stock called by feign!");
        return productMapper.unlockStock(productId, count);
    }

    @RequestMapping(value = "/deduct", method = RequestMethod.POST)
    @Transactional
    public Integer deductStock(@RequestParam("productId") Long productId, @RequestParam("count") Integer count) {
        log.info("deduct stock called by stock feign!");
        return productMapper.deductStock(productId, count);
    }
}
