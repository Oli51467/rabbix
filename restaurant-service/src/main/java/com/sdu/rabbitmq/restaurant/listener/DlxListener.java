package com.sdu.rabbitmq.restaurant.listener;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.common.commons.enums.OrderStatus;
import com.sdu.rabbitmq.common.domain.po.OrderDetail;
import com.sdu.rabbitmq.rdts.listener.AbstractDlxListener;
import com.sdu.rabbitmq.restaurant.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.restaurant.repository.OrderDetailMapper;
import com.sdu.rabbitmq.restaurant.repository.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Component
@Slf4j
public class DlxListener extends AbstractDlxListener {

    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Resource
    private ProductMapper productMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean receiveMessage(Message message) throws IOException {
        OrderMessageDTO orderMessage = objectMapper.readValue(message.getBody(), OrderMessageDTO.class);
        QueryWrapper<OrderDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", orderMessage.getOrderId());
        OrderDetail orderDetail = orderDetailMapper.selectOne(queryWrapper);
        if (!orderDetail.getStatus().equals(OrderStatus.ORDER_CREATED)) {
            // 将该订单关闭
            UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", orderMessage.getOrderId()).set("status", OrderStatus.FAILED.toString());
            orderDetailMapper.update(null, updateWrapper);
            productMapper.unlockStock(orderDetail.getProductId());
        }
        // 死信保存
        return false;
    }
}
