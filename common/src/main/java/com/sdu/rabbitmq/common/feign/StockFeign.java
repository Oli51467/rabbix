package com.sdu.rabbitmq.common.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-stock", url = "http://127.0.0.1:8870/product/stock")
public interface StockFeign {

    @PostMapping("/lock")
    Integer lockStock(@RequestParam Long productId, @RequestParam Integer count);

    @PostMapping("/unlock")
    Integer unlockStock(@RequestParam Long productId, @RequestParam Integer count);

    @PostMapping("/deduct")
    Integer deductStock(@RequestParam Long productId, @RequestParam Integer count);
}
