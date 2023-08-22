package com.sdu.rabbitmq.common.service.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sdu.rabbitmq.common.domain.po.Product;
import com.sdu.rabbitmq.common.repository.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Slf4j
@Service
public class IProductService {

    @Resource
    private ProductMapper productMapper;

    @Transactional
    public Integer lockStock(Long productId, Integer count) {
        log.info("lock stock called by feign!");
        return productMapper.lockStock(productId, count);
    }

    @Transactional
    public Integer unlockStock(Long productId, Integer count) {
        log.info("unlock stock called by feign!");
        return productMapper.unlockStock(productId, count);
    }

    @Transactional
    public Integer deductStock(Long productId, Integer count) {
        log.info("deduct stock called by stock feign!");
        return productMapper.deductStock(productId, count);
    }

    public Product queryById(Long productId) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", productId);
        return productMapper.selectOne(queryWrapper);
    }
}
