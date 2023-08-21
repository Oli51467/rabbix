package com.sdu.rabbitmq.common.feign;

import com.sdu.rabbitmq.common.domain.po.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-query", url = "http://127.0.0.1:8870/product/query")
public interface ProductQueryFeign {

    @GetMapping("/id")
    Product queryById(@RequestParam Long productId);
}
