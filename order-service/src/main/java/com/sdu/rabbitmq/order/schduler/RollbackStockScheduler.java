package com.sdu.rabbitmq.order.schduler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sdu.rabbitmq.order.entity.po.OrderDetail;
import com.sdu.rabbitmq.order.enums.OrderStatus;
import com.sdu.rabbitmq.order.mapper.ProductMapper;
import com.sdu.rabbitmq.order.repository.OrderDetailMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@EnableScheduling
@Configuration
@Component
@Slf4j
public class RollbackStockScheduler {

    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Resource
    private ProductMapper productMapper;

    @Scheduled(fixedDelayString = "5000")
    public void rollbackStock() {
        QueryWrapper<OrderDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", OrderStatus.FAILED.toString());
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(queryWrapper);
        for (OrderDetail orderDetail : orderDetails) {
            int flag = productMapper.unlockStock(orderDetail.getProductId());
            if (flag == 1) {
                orderDetailMapper.deleteById(orderDetail);
            }
        }
    }
}
