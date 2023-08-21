package com.sdu.rabbitmq.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sdu.rabbitmq.common.domain.po.Product;
import com.sdu.rabbitmq.product.repository.ProductMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/query")
public class QueryController {

    @Resource
    private ProductMapper productMapper;

    @RequestMapping(value = "/id", method = RequestMethod.GET)
    public Product queryProduct(@RequestParam Long productId) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", productId);
        return productMapper.selectOne(queryWrapper);
    }
}
