package com.sdu.rabbitmq.common.service.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sdu.rabbitmq.common.domain.po.Product;
import com.sdu.rabbitmq.common.repository.ProductMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class IProductService {

    @Resource
    private ProductMapper productMapper;

    public Integer lockStock(Long productId, Integer count) {
        return productMapper.lockStock(productId, count);
    }

    public Integer unlockStock(Long productId, Integer count) {
        return productMapper.unlockStock(productId, count);
    }

    public Integer deductStock(Long productId, Integer count) {
        return productMapper.deductStock(productId, count);
    }

    public Integer restoreStock(Long productId, Integer count) {
        return productMapper.restoreStock(productId, count);
    }

    public Product queryById(Long productId) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", productId);
        return productMapper.selectOne(queryWrapper);
    }
}
